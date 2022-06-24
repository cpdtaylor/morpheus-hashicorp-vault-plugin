package com.morpheusdata.vault

import com.morpheusdata.cypher.Cypher
import com.morpheusdata.cypher.CypherModule
import com.morpheusdata.cypher.CypherObject
import com.morpheusdata.vault.util.*
import groovy.util.logging.Slf4j
import com.morpheusdata.core.MorpheusContext
import groovy.json.JsonSlurper
import com.morpheusdata.core.Plugin

@Slf4j
class HashiCorpVaultCypherModule implements CypherModule {

  Cypher cypher
  private static final String KV1 = "KV1"
  private static final String KV2 = "KV2"
  private static final String DEFAULT_ENGINE = KV2 //legacy as the native Morpheus module only supported KV2
  private static final HashMap<String, AbstractVaultEngine> SUPPORTED_ENGINES = new HashMap<String, AbstractVaultEngine>(){{
      put(KV1, new Kv1VaultEngine())
      put(KV2, new Kv2VaultEngine())
  }}
  MorpheusContext morpheusContext
  Plugin plugin
  
  public void setMorpheusContext(MorpheusContext morpheusContext) {
    this.morpheusContext = morpheusContext
  }
  
  public void setPlugin(Plugin plugin) {
    this.plugin = plugin
  }
  
  @Override
  public void setCypher(Cypher cypher) {
      this.cypher = cypher
  }
  
  @Override
  public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
    if(value != null && value.length() > 0) {
      String key = relativeKey
      String vaultPath = relativeKey
      if(path != null) {
        key = path + "/" + key
      }
      if(relativeKey.startsWith("config/")) {
        return new CypherObject(key,value,0l, leaseObjectRef, createdBy)
      } else {
        String vaultUrl = this.getVaultUrl()
        String vaultToken = this.getVaultToken()
        
        AbstractVaultEngine vaultApiEngine = this.getVaultEngine(relativeKey)
        if (vaultApiEngine == null) {
          vaultApiEngine = SUPPORTED_ENGINES.get(DEFAULT_ENGINE)
        } else {
          vaultPath = this.getVaultPath(relativeKey) ?: relativeKey
        }
        Boolean success = vaultApiEngine.save(vaultPath, value, vaultUrl, vaultToken)
        if (success) {
          return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy)
        } else {
          return null
        }
      }
    } else {
        return null
    }
  }

  @Override
  public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
    String key = relativeKey
    String value
    String vaultPath = relativeKey
    if(path != null) {
      key = path + "/" + key
    }
    if(relativeKey.startsWith("config/")) {
      return null
    } else {
      String vaultUrl = this.getVaultUrl()
      String vaultToken = this.getVaultToken()

      AbstractVaultEngine vaultApiEngine = this.getVaultEngine(relativeKey)
      if (vaultApiEngine == null) {
        vaultApiEngine = SUPPORTED_ENGINES.get(DEFAULT_ENGINE)
      } else {
        vaultPath = this.getVaultPath(relativeKey) ?: relativeKey
      }
      
      value = vaultApiEngine.read(vaultPath, vaultUrl, vaultToken)
      
      try {
        CypherObject vaultResult = new CypherObject(key,value,leaseTimeout,leaseObjectRef, createdBy)
        vaultResult.shouldPersist = false
        return vaultResult

      } catch(Exception ex) {
        ex.printStackTrace()
        return null
      }
    }
  }

  @Override
  public boolean delete(String relativeKey, String path, CypherObject object) {
    if(relativeKey.startsWith("config/")) {
      return true
    } else {
      String vaultPath = relativeKey
      String vaultUrl = this.getVaultUrl()
      String vaultToken = this.getVaultToken()
      
      AbstractVaultEngine vaultApiEngine = this.getVaultEngine(relativeKey)
      if (vaultApiEngine == null) {
        vaultApiEngine = SUPPORTED_ENGINES.get(DEFAULT_ENGINE)
      } else {
        vaultPath = this.getVaultPath(relativeKey) ?: relativeKey
      }

      Boolean success = vaultApiEngine.delete(vaultPath, vaultUrl, vaultToken)
      
      return success
    }
  }

  @Override
  public String getUsage() {
    StringBuilder usage = new StringBuilder()

    usage.append("This allows secret data to be fetched to/from a HashiCorp Vault server. This can be configured in the Plugin settings.")

    return usage.toString()
  }

  @Override
  public String getHTMLUsage() {
    return null
  }

  private String getVaultUrl() {
    def settings = this.getSettings()
    if (settings.hashicorpVaultPluginUrl) {
      return settings.hashicorpVaultPluginUrl
    } else {
      return this.cypher.read("vault/config/url").value
    }
  }

  private String getVaultToken() {
    def settings = this.getSettings()
    if (settings.hashicorpVaultPluginUrl) {
      return settings.hashicorpVaultPluginToken
    } else {
      return this.cypher.read("vault/config/token").value
    }
  }
  
  private getSettings() {
    def settings = this.morpheusContext.getSettings(this.plugin)
    def settingsOutput = ""
    settings.subscribe(
      { outData -> 
        settingsOutput = outData
      },
      { error ->
        println error.printStackTrace()
      }
    )

    JsonSlurper slurper = new JsonSlurper()
    def settingsJson = slurper.parseText(settingsOutput)
    return settingsJson
  }
  
  private String getVaultPath(String relativePath) {
    String rtn = null
    if (relativePath != null) {
      String[] splitPath = relativePath.split('/', 2)
      if (splitPath.length > 1) {
        rtn = splitPath[1]
      }
    }
    return rtn
  }

  private AbstractVaultEngine getVaultEngine(String relativePath) {
    AbstractVaultEngine rtn = null
    if (relativePath != null) {
      String[] splitPath = relativePath.split('/')
      if (splitPath.length > 0) {
        String engine = splitPath[0].toUpperCase()
        rtn = SUPPORTED_ENGINES.get(engine)
      }
    }
    return rtn
  }

}
