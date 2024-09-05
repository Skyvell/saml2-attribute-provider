package io.curity.identityserver.plugin.ica.data.access.attributeimport AttributeValueimport AudienceRestrictionimport AuthnContextimport AuthnStatementimport Conditionsimport NameIDimport Subjectimport SubjectConfirmationimport SubjectConfirmationDataimport Responseimport com.onelogin.saml2.util.Utilimport io.curity.identityserver.plugin.ica.data.access.attribute.config.SAMLDataAccessProviderConfigurationimport org.json.JSONArrayimport org.json.JSONObjectimport org.slf4j.Loggerimport org.slf4j.LoggerFactoryimport se.curity.identityserver.sdk.attribute.AttributeTableViewimport se.curity.identityserver.sdk.attribute.Attributesimport se.curity.identityserver.sdk.datasource.AttributeDataAccessProviderimport se.curity.identityserver.sdk.errors.ErrorCodeimport standardAttributesimport java.time.Instantimport java.time.OffsetDateTimeimport java.time.ZoneIdimport java.time.ZoneOffsetimport java.util.*import Attribute as AttributeStatementimport kotlin.random.Randomclass SAMLAttributeDataAccessProvider(configuration: SAMLDataAccessProviderConfiguration):    AttributeDataAccessProvider {    companion object {        private val logger: Logger = LoggerFactory.getLogger(SAMLAttributeDataAccessProvider::class.java)    }    private val exceptionFactory = configuration.exceptionFactory    private val sessionManager = configuration.sessionManager    private val cryptoStore = configuration.cryptoStore    private val tokenEndpoint = configuration.tokenEndpoint.get()    private val pk = cryptoStore.privateKey    private val cert = cryptoStore.certificates[0]    override fun getAttributes(subject: String): AttributeTableView =        throw exceptionFactory.internalServerException(            ErrorCode.INVALID_INPUT,            "The plugin requires the use of the full subjectAttributes.")    override fun getAttributes(subjectMap: MutableMap<*, *>): AttributeTableView? {        val attributes: MutableList<Attributes> = mutableListOf()        val jwtFields = tokenizeJWT(subjectMap["token"].toString())        val idAss = "_" + generateRandomString(sessionManager.sessionId.length)        val idRes = "_" + generateRandomString(sessionManager.sessionId.length)        val assertion = assembleAssertion(jwtFields, idAss)        val response = assembleResponse(jwtFields, idRes)        var finAss: String        var finRes: String        when (subjectMap["signing"].toString()) {            "onlyAssertion" -> {                logger.info("Assertion signature requested, generating Signature")                finAss = assertion.sign(pk, cert)                response.setAssertion(finAss)                finRes = response.toXmlString()                logger.info("Only assertion signed: $finRes")            }            "onlyResponse" -> {                logger.info("Response signature requested, generating Signature")                finAss  = assertion.toXmlString()                response.setAssertion(finAss)                finRes = response.sign(pk, cert)                logger.info("Only response signed: $finRes")            }            "both" -> {                logger.info("Double signature requested, generating signature")                finAss = assertion.sign(pk, cert)                response.setAssertion(finAss)                finRes = response.sign(pk, cert)                logger.info("Both signed: $finRes")            }            else -> {                logger.info("no signature")                finAss = assertion.toXmlString()                response.setAssertion(finAss)                finRes = response.toXmlString()                logger.info("Response and assertion unsigned: $finRes")            }        }        // Base64Encode assertion and response.        finAss = Util.base64encoder(finAss)        finRes = Util.base64encoder(finRes)        // Create attributes and put in a list of attributes.        val attribute = Attributes.of("assertion", finAss)        sessionManager.put(attribute["assertion"])        val attribute2 = Attributes.of("response", finRes)        sessionManager.put(attribute2["response"])        attributes.add(attribute)        attributes.add(attribute2)        return AttributeTableView.ofAttributes(attributes)    }    private fun assembleResponse(jwtFields: List<String>, id: String): Response {        logger.info("Assembling Response")        val token = JSONObject(base64UrlDecode(jwtFields[1]))        val iat = unixToISO8601UTC((token.get("iat") as Int).toLong()).toString()        val response = Response(id, iat)        response.setIssuer(token.get("samlIssuer").toString())        return response    }    private fun assembleAssertion(jwtFields: List<String>, id: String): Assertion {        logger.info("Assembling assertion")        val token = JSONObject(base64UrlDecode(jwtFields[1]))        val iat = unixToISO8601UTC((token.get("iat") as Int).toLong()).toString()        val exp = unixToISO8601UTC((token.get("exp") as Int).toLong()).toString()        val assertion = Assertion(id, iat) // init assertion, convert timestamp to Date        // Questions - aud can be a single string. If this is the case this fails with cast error.        val audArr = token.get("aud") as JSONArray        val audList = audArr.toList()        var aud: String? = ""        for (field in audList) {            logger.info(field.toString())            if (field != tokenEndpoint && field != token.get("azp")) {                aud = field.toString()            }        }        // Hardcoded values        assertion.setIssuer(token.get("samlIssuer").toString())        assertion.setSubject(            Subject(                NameID(token.get("sub").toString()),                SubjectConfirmation(SubjectConfirmationData("", exp, token.get("samlRecipient").toString()))            ) // IF USE-CASE for full SAML-flow, must match ID of saml-request        )        assertion.setConditions(            Conditions(                AudienceRestriction(aud),                iat,                exp            )        )        assertion.setAuthnStatement(            AuthnStatement(                iat,                null, // IF USE-CASE for full SAML-flow, must match ID of saml-request                exp,                AuthnContext(token.get("amr").toString()) // "https://id.sambi.se/loa/loa3"            )        )        // Regex to find friendly-name of attributes        val regex = """^(?:https?:\/\/)?(?:[^\/]+\/)*([^\/]+)$""".toRegex()        // Ignore standard openid-claims        for (field in token.toMap()) {            if (!standardAttributes.contains(field.key)) {                assertion.addAttribute(                    AttributeStatement(                        AttributeValue(token.get(field.key).toString()),                        regex.find(field.key)?.groupValues?.get(1),                        field.key                    )                ) // staticUri            }        }        return assertion    }    private fun tokenizeJWT(token: String): List<String> {        val jwtFields = token.split('.')        if (jwtFields.size != 3)            throw Exception("Invalid JWT token")        return jwtFields    }    private fun unixToISO8601UTC(s: Long): OffsetDateTime? {        return Instant.ofEpochSecond(s)            .atZone(ZoneId.systemDefault())            .toLocalDateTime().atOffset(ZoneOffset.UTC)    }    private fun base64UrlDecode(token: String): String {        val decodedBytes: ByteArray = Base64.getUrlDecoder().decode(token)        return String(decodedBytes)    }    private fun generateRandomString(length: Int): String {        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"        val random = Random.Default        return (1..length)            .map { chars[random.nextInt(chars.length)] }            .joinToString("")    }}