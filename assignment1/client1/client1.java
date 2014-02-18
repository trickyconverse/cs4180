import java.io.*;
import java.lang.*;
	
public class client1{
	private static void argLength(String[] args){//checks for insufficient input size
		if (args.length != 6){ 
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA	private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file	name>");
			System.out.println("client1 requires 6 input parameters.");
			System.exit(0);
		}
	}
	private static String serverIpTest(String ipAddress){//checks for an valid server IP
		if (ipAddress.matches("^.[0-9]{1,3}\\..[0-9]{1,3}\\..[0-9]{1,3}\\..[0-9]{1,3}") != true){
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file name>");
			System.out.println("<server ip address>: Please input a valid IPv4 server address ([0-255].[0-255].[0-255].[0-255]).");
			System.exit(0);
		}
		return ipAddress;
	}
	private static int portTest(String port) throws NumberFormatException{ //checks for a valid integer for client 1 port number
		try {
			int serverPort = Integer.parseInt(port);
			return serverPort;
		} catch (NumberFormatException e){
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file name>");
			System.out.println("<port number client1>: Please input a valid integer.");
			System.exit(0);
		}
		return 0;
	}
	private static String passwordTest(String password){
		if (password.length() != 16){//checks for a 16 character password
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file name>");
			System.out.println("<client1 password>: Please input a password of exactly 16 characters.");
			System.exit(0);
		}
		if (password.matches("^[a-zA-Z0-9,./<>?;:.\"[]{}\\|!@#$%.&*()-_=+]*]$")){ // checks for valid characters
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file name>");
			System.out.println("<client1 password>: Please only use alphanumeric characters as well as the following symbols:");
			System.out.println(" , . / < > ? ; : . \" [ ] { } \\ | ! @ # $ % . & * ( ) - _ = +");
			System.exit(0);
		}
		return password;
	}
	private static File fileTest(String fileName, String input){
		File file = new File(fileName);
		if (!file.exists()){
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1.s RSA private exponent and modulus> <file containing client2.s RSA public exponent and modulus> <file name>");
			System.out.println(input + ": Input a valid file");
			System.exit(0);
		}
		return file;
	}
	public static void main(String[] args){
	/*	Input: 	<server ip address> <port number client1> <client1 password>
	 *		<file containing client1's RSA private exponent and modulus>
	 *		<file containing client2's RSA public exponent and modulus> 
	 *		<file name> -- 6 inputs*/

		//Check for invalid/garbage/missing input
		argLength(args);//checks for insufficient input size
		//<server ip address>
		String serverIP = serverIpTest(args[0]); //checks for an valid server IP
		//<port number client1>
		int serverPort = portTest(args[1]);//checks for a valid integer for client 1 port number
		//<client1 password>
		String password = passwordTest(args[2]); //checks for a valid, 16 character password
		//< file containing client1.s RSA public exponent and modulus>
		//make sure the client1's RSA file exists
		File RSA1 = fileTest(args[3], "<file containing client1.s RSA public exponent and modulus>");
		//<file containing client2.s RSA public exponent and modulus>
		//make sure the client2's RSA file exists
		File RSA2 = fileTest(args[4], "<file containing client2.s RSA public exponent and modulus>");
		//<file name>
		//make sure the file to be encrypted exists
		File Data = fileTest(args[5], "<file name>");
		//Encrypt file with AES in CBC
		Cipher aesCBC = Cipher.getInstance("AES/CBC/NoPadding"); //instantiate AES with CBC
		aesCBC.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(password.getBytes("UTF-8"), "AES"), new IvParameterSpec(new byte[16])); //initialize
		FileInputStream is = new FileInputStream(Data); //to be used for plaintext hashing
		CipherInputStream cis = new CipherInputStream(is, aesCBC); //encrypt data from file
		BufferInputStream bis = new BufferInputStream(cis); //buffered for use in socket transmission
		//Hash plaintext with SHA-256 & encrypt hash with RSA (private key)
		Signature SHA256 = Signature.getInstance("SHA256withRSA"); //instantiate SHA-256 with RSA
		byte[] encodedKeyPriv = new byte[(int)RSA1.length()]; //the encoded version of the private key will be read into this array
		new FileInputStream(RSA1).read(encodedKeyPriv); //the key contained in this file is read to the array
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedKeyPriv); //create a private key specification from the encoded key
		KeyFactory kf = KeyFactory.getInstance("RSA"); //instatiate RSA for private key
		PrivateKey pk = kf.generatePrivate(privateKeySpec); //generate the private key
		SHA256.initSign(pk); //initialize the signature process
		byte[] dataArrayPlain = new byte[(int) Data.length()]; //array for plaintext to be written to
		is.read(dataArrayPlain, 0, dataArrayPlain.length); //read in plaintext
		SHA256.update(dataArrayPlain); //update the signature with the plaintext
		byte[] dataArraySign = SHA256.sign(); //sign the plaintext
		//Encrypt password with client2 RSA key
		byte[] encodedKeyPub = new byte[(int)RSA2.length()];//the encoded version of the public key will be read into this array
		new FileInputStream(RSA2).read(encodedKeyPub); //the key contained in this file is read to the array
		X509EncodedKeySpec publicKeySpec =  new X509EncodedKeySpec(encodedKeyPub); //create a public key specification from the encoded key
		KeyFactory kf2 = Keyfactory.getInstance("RSA"); //instantiate RSA for public key
		PublicKey pk2 = kf2.generatePublic(publicKeySpec); //generate the public key
		Cipher RSA = Cipher.getInstance("RSA"); //instantiate RSA
		RSA.init(Cipher.ENCRYPT_MODE, pk2); //initialize RSA encryption
		byte[] passEncr = RSA.doFinal(password.getBytes()); //encrypt the password
		//Send Encrypted data, signature, and password to the server
		try{
			Socket socket = new Socket(serverIP, serverPort);//connect to the server
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			int count;
			while ((count = bis.read(dataArrayAES)) > 0){//read in buffered ciphertext
				out.write(dataArrayAES, 0, count); //write encrypted file to output buffer
			}
			out.flush(); //clear the buffer in preparation for signature
			out.write(dataArraySign, 0, dataArraySign.length);//write signature to server
			out.flush();
			out.write(passEncr, 0, passEncr.length);//write encrypted password to server
			out.flush();
			out.close();
			socket.close();

		} catch (IOException e){
			System.out.println("Input: <server ip address> <port number client1> <client1 password> < file containing client1's RSA private exponent and modulus> <file containing client2's RSA public exponent and modulus> <file name>");
			System.out.println("<server ip address>: Input a valid server address.");
			System.exit(0);
		}
		bis.close();
		cis.close();
		fis.close();
		//Encrypt password with client2 RSA key
		//Send encrypted password to client 2 via server
		//Disconnect from server after sending password, file, and signature
	}
}
