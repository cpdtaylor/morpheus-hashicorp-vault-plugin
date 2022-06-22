package com.morpheusdata.vault.util;
import com.morpheusdata.cypher.util.RestApiUtil;
import com.morpheusdata.cypher.util.ServiceResponse;

abstract class AbstractVaultEngine {

    public static final String DEFAULT_API_VERSION = "v1";

    public abstract boolean delete(String vaultPath, String vaultUrl, String vaultToken);

    public abstract String read(String vaultPath, String vaultUrl, String vaultToken);

    public abstract boolean save(String vaultPath, String value, String vaultUrl, String vaultToken);
    
    public abstract String getDescription();
    
    public abstract String getName();

    private String getBasePath(String version) {
        if (version == null) {
            version = AbstractVaultEngine.DEFAULT_API_VERSION;
        }
        return "/" + version + "/"
    }

    private ServiceResponse _delete(String vaultPath, String vaultUrl, String vaultToken) {
        RestApiUtil.RestOptions restOptions = this.createRestApiOptions(vaultToken);
        vaultPath = this.getBasePath() + vaultPath;
        try {
            ServiceResponse resp = RestApiUtil.callApi(vaultUrl,vaultPath,null,null,restOptions,"DELETE");
            return resp;
        } catch(Exception ex) {
            return null;
        }
    }

    private ServiceResponse _save(String vaultPath, String body, String vaultUrl, String vaultToken) {
        RestApiUtil.RestOptions restOptions = this.createRestApiOptions(vaultToken);
        vaultPath = this.getBasePath() + vaultPath;
        restOptions.body = body;
        try {
            ServiceResponse resp = RestApiUtil.callApi(vaultUrl, vaultPath, null, null, restOptions, "POST");
            return resp;
        } catch(Exception ex) {
            return null;
        }
    }

    private ServiceResponse _read(String vaultPath, String vaultUrl, String vaultToken) {
        RestApiUtil.RestOptions restOptions = this.createRestApiOptions(vaultToken);
        vaultPath = this.getBasePath() + vaultPath;
        try {
            ServiceResponse resp = RestApiUtil.callApi(vaultUrl, vaultPath, null, null, restOptions, "GET");
            return resp;
        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private RestApiUtil.RestOptions createRestApiOptions(String vaultToken) {
        RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
        restOptions.headers = new LinkedHashMap<>();
        this.setAuthenticationHeaders(restOptions, vaultToken);
        restOptions.contentType = "application/json";
        return restOptions;
    }

    private setAuthenticationHeaders(RestApiUtil.RestOptions restOptions, String vaultToken) {
        restOptions.headers.put("X-VAULT-TOKEN", vaultToken);
    }
}