package com.example.npci.service;

import com.example.npci.Repository.PspRegistryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class NpciService {
    private final PspRegistryRepository pspRegistryRepository;

    @Value("${security.npci-bank-private-key}")
    private String privateKey;

    @Value("${security.npci-bank-public-key}")
    private String publicKey;

    public NpciService(PspRegistryRepository pspRegistryRepository) {
        this.pspRegistryRepository = pspRegistryRepository;
    }

    public String loadPublicKeyFromdb(String pspId){
        return pspRegistryRepository.findByPspId(pspId).get().getPublic_key();
    }

    public PublicKey convertToPublicKey(String base64Key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
        catch(Exception e) {
            throw new IllegalStateException("failed to load public key",e);
        }
    }

    public boolean verify(
            String payload,
            String signatureBase64,
            PublicKey publicKey
    ){
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));

            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(sigBytes);
        }
        catch(Exception e) {
            throw new IllegalStateException("failed to verify signature",e);
        }
    }


//    public PrivateKey loadPrivateKey(String path) throws Exception {
//        InputStream is = getClass()
//                .getClassLoader()
//                .getResourceAsStream(path);
//
//        if (is == null) {
//            throw new RuntimeException("Public key file not found");
//        }
//
//        String privateKeyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//
//        privateKeyPem = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replaceAll("\\s", "");
//
//        byte[] decoded = Base64.getDecoder().decode(privateKeyPem);
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
//        KeyFactory kf = KeyFactory.getInstance("RSA");
//        return kf.generatePrivate(spec);
//    }

    public PrivateKey loadPrivateKey() throws Exception {
        String privateKeyPem = privateKey;

        privateKeyPem = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public String signPayload(String payload,PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }


}
