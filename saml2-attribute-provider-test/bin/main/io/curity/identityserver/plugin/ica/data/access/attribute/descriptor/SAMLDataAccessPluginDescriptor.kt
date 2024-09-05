//package io.curity.identityserver.plugin.ica.data.access.attribute.descriptor
//
//import io.curity.identityserver.plugin.ica.data.access.attribute.SAMLAttributeDataAccessProvider
//import io.curity.identityserver.plugin.ica.data.access.attribute.config.SAMLDataAccessProviderConfiguration
//import se.curity.identityserver.sdk.plugin.descriptor.DataAccessProviderPluginDescriptor
//
//class SAMLDataAccessPluginDescriptor : DataAccessProviderPluginDescriptor<SAMLDataAccessProviderConfiguration>{
//    override fun getPluginImplementationType() = SAMLAttributeDataAccessProvider::class.java.typeName
//
//    override fun getConfigurationType() = SAMLDataAccessProviderConfiguration::class.java
//
//    override fun getAttributeDataAccessProvider() = SAMLAttributeDataAccessProvider::class.java
//
//}