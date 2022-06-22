package com.morpheusdata.vault.util;
import com.morpheusdata.cypher.util.ServiceResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.fasterxml.jackson.databind.type.LogicalType.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class Kv2VaultEngine extends AbstractVaultEngine {

  public boolean delete(String vaultPath, String vaultUrl, String vaultToken) {
    this._delete(vaultPath, vaultUrl, vaultToken);
    return true;
  }

  public String read(String vaultPath, String vaultUrl, String vaultToken) {
    ServiceResponse resp = this._read(vaultPath, vaultUrl, vaultToken);
    if(resp.getSuccess()) {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<HashMap<String,Object>>() {};
        HashMap<String,Object> responseJson = mapper.readValue(resp.getContent(), typeRef);
        Map<String,Object> dataMap = (java.util.Map<String,Object>)(((java.util.Map<String,Object>)(responseJson.get("data"))).get("data"));
        String vaultResult = mapper.writeValueAsString(dataMap);
        return vaultResult;
    } else {
        return null;
    }
  }

  public boolean save(String vaultPath, String value, String vaultUrl, String vaultToken) {
    String body = "{\"data\": " + value + "}";
    ServiceResponse resp = this._save(vaultPath, body, vaultUrl, vaultToken)
    if(resp.getSuccess()) {
        return true;
    } else {
        return false;
    }
  }
  
  public String getDescription() {
    return "KV2 Vault Engine"
  }
  
  public String getName() {
    return "KV2"
  }
  
}