/**
 * 
 */
package com.oneid.rp;

import java.io.IOException;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author jgoldberg
 * 
 */
public class OneIDClient {

	protected String apiID;
	protected String apiKey;

	private static final String ONEID_HOST = "https://keychain.oneid.com/";

	/**
     * Constructor - API Credentials are required
     * 
     * Can be retrieved from https://keychain.oneid.com/register
     * 
     * @param apiKey
     * @param apiID
     */
	public OneIDClient(String apiKey, String apiID) {
		this.apiID = apiID;
		this.apiKey = apiKey;
	}

	public JSONObject open(String method) throws IOException {
		return open(method, "{}");
	}

	public JSONObject open(String method, String post) throws IOException {
		String basic = apiID + ":" + apiKey;
		String encoding = new String(Base64.encodeBase64(basic.getBytes(), false));
		
		HttpPost httpPost = new HttpPost(ONEID_HOST + method);
		httpPost.setEntity(new StringEntity(post));
		httpPost.addHeader("Authorization", "Basic " + encoding);
		HttpClient client = new DefaultHttpClient();

		HttpResponse response = client.execute(httpPost);
		String resultString = IOUtils.toString(response.getEntity().getContent());
		
		JSONObject result = (JSONObject) JSONSerializer.toJSON(resultString);
		return result;
	}

	/**
	 * Checks whether a OneID authentication was successfully verified.
	 * 
	 * @param response - a JSONObject representing a OneID authentication payload
	 * @return boolean - true if validated, false if validation failed
	 */
	public boolean isValidated(JSONObject response) {
		return "success".equals(response.getString("error")) && 0 == response.getInt("errorcode");
	}

	/**
	 * Create a OneID-formatted nonce
	 * 
	 * @return
	 * @throws IOException
	 */
	public String nonce() throws IOException {
		JSONObject json = open("make_nonce");
		return json.getString("nonce");
	}

	/**
	 * Validate a OneID authentication payload
	 * 
	 * @param payload - a String representing a OneID authentication payload
	 * @return
	 * @throws IOException - throws an IOException if an HTTP connection can't be opened
	 */
	public OneIDResponse validate(String payload) throws IOException {
		JSONObject validate = open("validate", payload);
		if (!isValidated(validate))
			return new OneIDResponse(false, validate.getString("error"), null);

		try {
			JSONObject inputJSON = (JSONObject) JSONSerializer.toJSON(payload);
			return new OneIDResponse(true, null, inputJSON);
		} catch (JSONException e) {
		}
		
		return new OneIDResponse(false, payload, null);
	}
}
