import java.security.KeyStore;
import java.security.*;
import java.io.*;
import java.security.cert.*;
import java.io.FileInputStream;
import javax.net.ssl.*;
import javax.net.ssl.SSLContext;

/* the socket class is a series of functions implementing the procedure outlined in the Palomino Labs Blog entitled
 * "Java 2-way TLS/SSL (Client Certificates) and PKCS12 vs JKS KeyStores). The Client and Server will both
 * use these functions, but will do so for their respective implementations.
 *
 * Link: http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
 * */

public class socket{
	//KeyStore: return keyStore to be used for sending/receiving certs (keyStore/trustStore)
	public static KeyStore keyStoreTLS(String keyStoreType, String keyStorePath, String password){
		KeyStore keyStore = null;
		try{
			FileInputStream inputStream = new FileInputStream(keyStorePath);
			keyStore = KeyStore.getInstance(keyStoreType);
			keyStore.load(inputStream, password.toCharArray());
			inputStream.close();
			return keyStore;
		} catch (FileNotFoundException e){
			storeError.invalidKeyStoreFile(keyStorePath);
		} catch (KeyStoreException e){
			storeError.invalidKeyStoreType(keyStoreType);
		} catch (IOException e){
			storeError.invalidKeyStoreData(keyStorePath);
		} catch (NoSuchAlgorithmException e){
			storeError.invalidAlgorithm(keyStorePath);
		} catch (CertificateException e){
			storeError.invalidCertificate(keyStorePath);
		}
		return keyStore;
	}
	//KeyManagerFactory: used to manage keyStore (sending certs)
	public static KeyManagerFactory keyManagerTLS(KeyStore keyStore, String password){
		KeyManagerFactory keyManagerFactory = null;
		try{
			keyManagerFactory = 
				KeyManagerFactory.getInstance("SunX509", "SunJSSE");
			keyManagerFactory.init(keyStore, password.toCharArray());
			return keyManagerFactory;
		} catch (NoSuchAlgorithmException e){
			managerError.invalidAlgorithm();
		} catch (NoSuchProviderException e){
			managerError.invalidProvider();
		} catch (IllegalArgumentException e){
			managerError.illegalArgument();
		} catch (NullPointerException e){
			managerError.nullPointer();
		} catch (KeyStoreException e){
			storeError.invalidKeyStore();
		} catch (UnrecoverableKeyException e){
			storeError.invalidKeyStore();
		} 
		return keyManagerFactory;
	}

	//TrustManagerFactory: used to manage trustStore (receiving certs)
	public static TrustManagerFactory trustManagerTLS(KeyStore trustStore){
		TrustManagerFactory trustManagerFactory = null;
		try{
			trustManagerFactory = 
				TrustManagerFactory.getInstance("PKIX", "SunJSSE");
			trustManagerFactory.init(trustStore);
			return trustManagerFactory;
		} catch (NoSuchAlgorithmException e){
			managerError.invalidAlgorithm();
		} catch (NoSuchProviderException e){
			managerError.invalidProvider();
		} catch (IllegalArgumentException e){
			managerError.illegalArgument();
		} catch (NullPointerException e){
			managerError.nullPointer();
		} catch (KeyStoreException e){
			managerError.trustStoreFail();
		}
		return trustManagerFactory;
	}
	//SSLContext: used for socketing
	public static SSLContext SSLContextTLS(KeyManagerFactory keyManagerFactory, TrustManagerFactory 
			trustManagerFactory, String password){
		SSLContext sslContext = null;
		try{
			//x509keymanager
			X509KeyManager x509KeyManager = null;
			for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
				if (keyManager instanceof X509KeyManager) {
					x509KeyManager = (X509KeyManager) keyManager;
					break;
				}
			}
			if (x509KeyManager == null) {
				throw new NullPointerException();
			}
			//x509trustmanager
			X509TrustManager x509TrustManager = null;
			for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
				if (trustManager instanceof X509TrustManager) {
					x509TrustManager = (X509TrustManager) trustManager;
					break;
				}
			}
	       		if (x509TrustManager == null) {
    				throw new NullPointerException();
			}
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(new X509KeyManager[]{x509KeyManager}, new X509TrustManager[]{x509TrustManager}, null);
			return sslContext;
		} catch (NoSuchAlgorithmException e){
			managerError.invalidAlgorithm();
		} catch (NullPointerException e){
			managerError.invalidProtocol();
		} catch (KeyManagementException e){
			managerError.invalidManager();
		} catch(IllegalStateException e){
			managerError.illegalState();
		}
		return sslContext;

	}
/* the storeError class contains all functions used to generate unique store error messages for the socket
 * class.
 */
	private static class storeError{
		//invalidKeyStoreFile: prints error message for invalid keyStore file.
		private static void invalidKeyStore(){
			System.out.println("Keystore not found.");
			System.exit(1);
		}
		private static void invalidKeyStoreFile(String keyStorePath){
			System.out.println("Given keyStorePath: " + keyStorePath);
			System.out.println("Please use a valid path for the keystore.");
			System.exit(1);
		}
		//invalidKeyStoreType: prints error message for invalid keyStore type
		private static void invalidKeyStoreType(String keyStoreType){
			System.out.println("Given keyStoreType: " + keyStoreType);
			System.out.println("keyStoreType is either \"JKS\" or \"PKCS12\"");
			System.exit(1);
		}
		private static void invalidKeyStoreData(String keyStorePath){
			System.out.println("Problem with keystore found at " + keyStorePath);
			System.exit(1);
		}
		private static void invalidAlgorithm(String keyStorePath){
				System.out.println("The algorithm used to check the integrity of the keystore found at " + keyStorePath + " cannot be found.");
			System.exit(1);
		}
		private static void invalidCertificate(String keyStorePath){
			System.out.println("One or more of the certificates in the keystore found at "
					+ keyStorePath + "could not be loaded.");
			System.exit(1);
		}
	}


/* the managerError class contains all functions used to generate unique manager error messages for the 
 * socket class.
 */ 
	private static class managerError{
		private static void invalidAlgorithm(){ //since directly specified, this should not ever occur.
			System.out.println("Unknown trust manager algorithm issue.");
			System.exit(-1);
		}
		private static void invalidProvider(){ //since directly specified, this should not ever occur.
			System.out.println("Unknown trust manager provider issue.");
			System.exit(-1);
		}
		private static void illegalArgument(){ //since directly specified, this should not ever occur.
			System.out.println("Provider name null/empty.");
			System.exit(-1);
		}
		private static void nullPointer(){//since directly specified, this should not ever occur.
			System.out.println("Algorithm is null.");
			System.exit(-1);
		}
		private static void trustStoreFail(){
			System.out.println("Trust store init failed.");
			System.exit(1);
		}
		private static void illegalState(){
			System.out.println("Factory not initialized.");
			System.exit(1);
		}
		private static void invalidProtocol(){ //since directly specified, this should not ever occur.
			System.out.println("Protocol is null");
			System.exit(-1);
		}
		private static void invalidManager(){
			System.out.println("Failed context initialization.");
			System.exit(1);
		}
	}

	public static void main(String[] args){
		//tested for client
		//keystore
		String keyStoreType = "JKS";
		String keyStorePath = "client/client.keystore";
		String keyStorePassword = "client";
		KeyStore keyStore = keyStoreTLS(keyStoreType, keyStorePath, keyStorePassword);
		KeyManagerFactory keyManagerFactory = keyManagerTLS(keyStore, keyStorePassword);
		//truststore
		String trustStorePath = "client/clienttrust.keystore";
		String trustStorePassword = keyStorePassword;
		KeyStore trustStore = keyStoreTLS(keyStoreType, trustStorePath, trustStorePassword);
		TrustManagerFactory trustManagerFactory = trustManagerTLS(trustStore);
		//bring it all together
		SSLContext sslContext =	
			SSLContextTLS(keyManagerFactory, trustManagerFactory, keyStorePassword);
	}
	
}