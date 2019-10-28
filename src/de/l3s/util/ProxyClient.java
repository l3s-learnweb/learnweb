package de.l3s.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

public class ProxyClient
{
    public static Logger log = Logger.getLogger(ProxyClient.class);

    public static void main(String[] args) throws Exception
    {
        /*
        generatePrivateKey();
        System.exit(0);
        */
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        final String rawPrivateKey = "30820275020100300D06092A864886F70D01010105000482025F3082025B020100028181009A229D5A343C243C52ACBD581FAD282603C2B461A43BC61EF230ECAD9559580A0A48B6DF3A3B1FAAC0AAAE6564BF17E5D24761461F3E860656E692267623DE53FC2817C019D65C77A40AAA9FDDD406C1BDBDEFEA99F7F241502E0436427B70795E5D47B8C711393D895D2D2C7003FA64636B54B35F33C90121F6D28A48C6FC81020301000102818053D6661E4D71350876B3AC9DC5450B247A412A9A3D99A1AEED6F3D1D41B202181BF5E73A4E53206B6136B57B432C49D460E7207AE2BDC06AAC53E7C4F1D79AB2DD4D59E4B1FCDEF90F95A60ED6D4EAA129798F4DE0E32A67EE789234FBB2EDB5072BC013C859A5333A1A343366A6A096C70CE92A990834AADFA9C64A2E866BBD024100D46AAB5392D6CEC24B66D7A84134D07372DF74158207E3A686A3A66065BA5194B01BA59F956CDC9C0DC22EFB9E89E45AB3E9AF2889B726858B29B00754A946D7024100B9C2ABFCC13E5B372D58365B29B81D4713035CCA69C732FFFBABD030AA903450EBECFF40DE6986DBEE5ED11443FC770CDAEF7536D2AE274C236937F1088CE46702402FDF4271795C740891D9C1ACA7D5714D338C6CAB143D16EF46D4C7005EACD909FB8E9F8B11B011201271BB08F637F980FF9F20DCAFFCD8EFABE75F46A7ABE6CD024051E1959E1C303854E20FF825C705F921D327B04728C93D99C87DBB8F381FC86FC30EFC94C3751094F145AA339CA43475DE2B9F274346B0DBC6E0226084BC5611024079991A0EC173C2AD1CA48A38E6214F8102E317FB30B4B8AA22CC2A831B02470FBBBC4A22CA4B794FD3E89DCE5DA0DB31D895C62922A2AAFA6E780A5818A11B6D";
        final PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(DatatypeConverter.parseHexBinary(rawPrivateKey)));

        RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        log.debug(DatatypeConverter.printHexBinary(publicKey.getEncoded()));
        log.debug(publicKey.getEncoded().length);

        final String url = "http://www.google.com";
        final String userId = "23478123743";
        final long clientId = 1;
        final String payload = url + userId + clientId;

        final Signature signature = Signature.getInstance("SHA224withRSA");
        signature.initSign(privateKey);
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signature.sign();
        log.debug(DatatypeConverter.printBase64Binary(sig));
    }

    @SuppressWarnings("unused")
    public static void generatePrivateKey() throws Exception
    {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();

        log.debug(DatatypeConverter.printBase64Binary(privateKey));
        log.debug(privateKey.length);
    }

}
