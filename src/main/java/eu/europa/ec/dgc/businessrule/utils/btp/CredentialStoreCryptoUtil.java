/*-
 * ---license-start
 * EU Digital COVID Certificate Business Rule Service / dgca-businessrule-service
 * ---
 * Copyright (C) 2021 - 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.businessrule.utils.btp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("btp")
public class CredentialStoreCryptoUtil {

    @Value("${sap.btp.credstore.clientPrivateKey}")
    private String clientPrivateKeyBase64;

    @Value("${sap.btp.credstore.serverPublicKey}")
    private String serverPublicKeyBase64;

    @Value("${sap.btp.credstore.encrypted}")
    private boolean encryptionEnabled;

    private PrivateKey ownPrivateKey;

    @PostConstruct
    private void prepare() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!encryptionEnabled) {
            return;
        }

        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder()
            .decode(clientPrivateKeyBase64));
        this.ownPrivateKey = rsaKeyFactory.generatePrivate(pkcs8EncodedKeySpec);

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder()
            .decode(serverPublicKeyBase64));
        PublicKey serverPublicKey = rsaKeyFactory.generatePublic(x509EncodedKeySpec);
    }

    protected String decrypt(String jweResponse) {
        if (!encryptionEnabled) {
            return jweResponse;
        }

        JWEObject jweObject;

        try {
            RSADecrypter rsaDecrypter = new RSADecrypter(ownPrivateKey);
            jweObject = JWEObject.parse(jweResponse);
            jweObject.decrypt(rsaDecrypter);

            Payload payload = jweObject.getPayload();
            return payload.toString();
        } catch (ParseException | JOSEException e) {
            log.error("Failed to parse JWE response: {}.", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
