import java.io.*;
import java.net.*;

public class UDPClient {

    private static InetAddress serverAddress;
    private static DatagramSocket socket;

    static {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            serverAddress = InetAddress.getByName(Config.SERVER_ADDRESS);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        //Aby nawiazac polaczenie TCP musimy wyslac 3 pakiety na 3 pierwsze porty serwera wraz z ich indeksem w tablicy

        System.out.println(serverAddress);

        sentPackage(1024,"0");
        Thread.sleep(100);
        sentPackage(1025,"1");
        Thread.sleep(100);
        sentPackage(1026,"2");
        Thread.sleep(100);


        String ret = receivePackage();
        System.out.println(ret);




        if(!ret.equals("BLAD")) {
            InetAddress serverTCP = InetAddress.getByName("localhost");
            Socket joinsocket = new Socket(serverTCP, Integer.parseInt(ret));

            InputStream sis = joinsocket.getInputStream();
            OutputStream sos = joinsocket.getOutputStream();
            InputStreamReader sisr = new InputStreamReader(sis);
            OutputStreamWriter sosr = new OutputStreamWriter(sos);
            BufferedReader br = new BufferedReader(sisr);
            BufferedWriter bw = new BufferedWriter(sosr);

            bw.write("TCP Connection succesfully");
            bw.newLine();
            bw.flush();
            System.out.println(br.readLine());
        }

    }

    public static void sentPackage(int port,String message) throws IOException {
         //Otwarcie gniazda
        byte[] stringContents = message.getBytes("utf8"); //Pobranie strumienia bajtów z wiadomosci

        DatagramPacket sentPacket = new DatagramPacket(stringContents, stringContents.length);
        sentPacket.setAddress(serverAddress);
        sentPacket.setPort(port);
        socket.send(sentPacket);

    }

    public static String receivePackage() throws IOException {
        DatagramPacket reclievePacket = new DatagramPacket( new byte[Config.BUFFER_SIZE], Config.BUFFER_SIZE);

        socket.setSoTimeout(2000);
        try{
            socket.receive(reclievePacket);
            int length = reclievePacket.getLength();
            String message =
                    new String(reclievePacket.getData(), 0, length, "utf8");
            System.out.println("Serwer wysłał port");
            return message;

        }catch (SocketTimeoutException ste){
            System.out.println("Sekwencja jest niepoprawna, serwer nie odpowiedzial");
            return "BLAD";
        }
    }
}
