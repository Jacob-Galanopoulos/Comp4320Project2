import java.net.*;  // for DatagramSocket, DatagramPacket, and InetAddress
import java.io.*;   // for IOException
import java.nio.*;
import java.util.Arrays;

/*
 * Emma Mills and Jake Galanopoulos Project 2
 * Group 2 Project
 */

public class ServerUDP {

   private static final int ECHOMAX = 10;  // Maximum size of echo datagram

   public static void main(String[] args) throws IOException {
   
      if (args.length != 1)  // Test for correct argument list
         throw new IllegalArgumentException("Parameter(s): <Port>");
   
      int servPort = Integer.parseInt(args[0]);
   
      DatagramSocket socket = new DatagramSocket(servPort);
      DatagramPacket packet = new DatagramPacket(new byte[ECHOMAX], ECHOMAX);
   
      for (;;) {  // Run forever, receiving and repying to datagrams
         socket.receive(packet);     // Receive packet from client
         System.out.println("Handling client at " +
            packet.getAddress().getHostAddress() + " on port " + packet.getPort());
            
         /* Steps:
          * 1. Get values from Request
          * 2. Check Length of Request matches TML
          *       if not matching: errorCode = 127
          *       else continue
          * 3. Check checksum
          *       if incorrect: errorCode = 63
          *       else continue
          * 4. If passed #2 & 3: Calculate P(x)
          * 5. Create Checksum
          * 6. Create Server Response
          */
          
          //1.)
         byte[] rawData = packet.getData();
         //System.out.println("RawData = " + Arrays.toString(rawData));
         
         ByteBuffer bb = ByteBuffer.wrap(rawData);
         byte TML = bb.get();
         short requestID = bb.getShort();
         byte x = bb.get();
         byte a4 = bb.get();
         byte a3 = bb.get();
         byte a2 = bb.get();
         byte a1 = bb.get();
         byte a0 = bb.get();
         byte checksum = bb.get();
         
         System.out.println();
         System.out.println("Data Recieved In Hexadecimal:");
         System.out.println("TML = 0x" + String.format("%02X ", TML).toUpperCase());
         System.out.println("Request ID = 0x" + Integer.toHexString(requestID & 0xffff).toUpperCase());
         System.out.println("x = 0x" + String.format("%02X ", x).toUpperCase());
         System.out.println("a4 = 0x" + String.format("%02X ", a4).toUpperCase());
         System.out.println("a3 = 0x" + String.format("%02X ", a3).toUpperCase());
         System.out.println("a2 = 0x" + String.format("%02X ", a2).toUpperCase());
         System.out.println("a1 = 0x" + String.format("%02X ", a1).toUpperCase());
         System.out.println("a0 = 0x" + String.format("%02X ", a0).toUpperCase());
         System.out.println("Checksum = 0x" + String.format("%02X ", checksum).toUpperCase());
          
          //2. & 3. & 4.)
         int result = 0;
         byte errorCode = 0;
         if ( (int)TML == 10) {
            
            byte[] recievedDataWithoutChecksum = Arrays.copyOfRange(rawData, 0, 9);
            //System.out.println("Manipulated Data = " + Arrays.toString(recievedDataWithoutChecksum));
            long actualChecksum = calculateChecksum( recievedDataWithoutChecksum );
            //System.out.println("Computed Checksum = " + actualChecksum);
            long comparableChecksum = checksum;
            
            if ((byte)actualChecksum == (byte)comparableChecksum) {
            
               result = polyCalc(x, a4, a3, a2, a1, a0);
            
            } else {
               errorCode = 63;
            }
         } else {
            errorCode = 127;
         }
         
         //5.)
         ByteBuffer bytesToCheck = ByteBuffer.allocate(8);
         bytesToCheck.put((byte)9);
         bytesToCheck.putShort(requestID);
         bytesToCheck.put(errorCode);
         bytesToCheck.putInt(result);
         
         byte responseChecksum = (byte)calculateChecksum(bytesToCheck.array());
         
         //6.)
         ByteBuffer bytesToReturn = ByteBuffer.allocate(9);
         bytesToReturn.put((byte)9);
         bytesToReturn.putShort(requestID);
         bytesToReturn.put(errorCode);
         bytesToReturn.putInt(result);
         bytesToReturn.put(responseChecksum);
         
         byte[] bytesToSend = bytesToReturn.array();
         
         System.out.println();
         System.out.println("Data Sent In Hexadecimal:");
         System.out.println("TML = 0x" + Integer.toHexString(9).toUpperCase());
         System.out.println("Request ID = 0x" + Integer.toHexString(requestID & 0xffff).toUpperCase());
         System.out.println("Error Code = 0x" + String.format("%02X ", errorCode).toUpperCase());
         System.out.println("Result = 0x" + Integer.toHexString(result).toUpperCase());
         System.out.println("Checksum = 0x" + String.format("%02X ", responseChecksum).toUpperCase());
      
         
         DatagramPacket responsePacket = new DatagramPacket(bytesToSend, bytesToSend.length, 
                                                            packet.getAddress(), packet.getPort());
         
         socket.send(responsePacket);       // Send the same packet back to client
         packet.setLength(ECHOMAX); // Reset length to avoid shrinking buffer
      }
   }
   
   private static int polyCalc(byte x, byte a4, byte a3, byte a2, byte a1, byte a0) {
      double doubx = x;
      int degree4 = a4 * (int)Math.pow(doubx,4);
      int degree3 = a3 * (int)Math.pow(doubx,3);
      int degree2 = a2 * (int)Math.pow(doubx,2);
      int degree1 = a1 * (int)doubx;
      int degree0 = a0;
      
      return (degree4 + degree3 + degree2 + degree1 + degree0);
   }
   
   private static long calculateChecksum(byte[] buf) {
      int length = buf.length;
      int i = 0;
   
      long sum = 0;
      long data;
   
      while (length > 1) {
         data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
         sum += data;
         if ((sum & 0xFFFF0000) > 0) {
            sum = sum & 0xFFFF;
            sum += 1;
         }
      
         i += 2;
         length -= 2;
      }
      if (length > 0) {
         sum += (buf[i] << 8 & 0xFF00);
         if ((sum & 0xFFFF0000) > 0) {
            sum = sum & 0xFFFF;
            sum += 1;
         }
      }
   
      sum = ~sum;
      sum = sum & 0xFFFF;
      return sum;
   }
}
