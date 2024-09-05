package io.curity.identityserver.plugin.ica.data.access.attribute.config

import se.curity.identityserver.sdk.config.Configuration
import se.curity.identityserver.sdk.config.annotation.DefaultString
import se.curity.identityserver.sdk.config.annotation.Description
import se.curity.identityserver.sdk.service.*
import se.curity.identityserver.sdk.service.crypto.AsymmetricSigningCryptoStore
import java.util.Optional


interface SAMLDataAccessProviderConfiguration : Configuration {

    val sessionManager: SessionManager
    val exceptionFactory: ExceptionFactory
    val cryptoStore : AsymmetricSigningCryptoStore

    @get:Description("Token Endpoint")
    val tokenEndpoint: Optional<String>

}
