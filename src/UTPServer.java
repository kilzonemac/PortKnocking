import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UTPServer {

    private static ExecutorService threads = Executors.newCachedThreadPool();
    private static InetAddress serverAddress;
    static {
        try {
            serverAddress = InetAddress.getByName(Config.SERVER_ADDRESS);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static Map<String,List<Boolean>> clients = new ConcurrentHashMap<>();

    static class ListeningPort implements Runnable {
        private DatagramSocket datagramSocket;
        private int port;

        public ListeningPort(int port) throws SocketException {
            this.datagramSocket = new DatagramSocket(port,serverAddress);
            this.port=port;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    DatagramPacket reclievedPacket
                            = new DatagramPacket(new byte[Config.BUFFER_SIZE], Config.BUFFER_SIZE);

                    datagramSocket.receive(reclievedPacket);

                    int length = reclievedPacket.getLength();
                    String message =
                            new String(reclievedPacket.getData(), 0, length, "utf8");

                    // Port i host który wysłał nam zapytanie
                    InetAddress address = reclievedPacket.getAddress();
                    int port = reclievedPacket.getPort();

                    List<Boolean> client_sequenction;

                    if(clients.containsKey(address.getHostAddress()+":"+port)) {
                        client_sequenction=clients.get(address.getHostAddress()+":"+port);

                    }
                    else {
                        client_sequenction  = Arrays.asList(false,false,false);
                        client_sequenction = Collections.synchronizedList(client_sequenction);
                        clients.put(address.getHostAddress()+":"+port,client_sequenction);
                    }

                    if(Config.PORTS.indexOf(this.port)==Integer.parseInt(message)){

                        client_sequenction.set(Integer.parseInt(message),true);
                    }


                //RESPONSE
                    for (String key : clients.keySet()) {
                        List<Boolean> sequence = clients.get(key);

                        int counter = 0;
                        for (Boolean bool : sequence) {
                            if (bool)
                                counter++;

                        }

                        if (counter == 3) {
                            System.out.println("NAWIAZUJEMY POLACZENIE");
                            log("Server socket creation");
                            ServerSocket serverSocket = new ServerSocket(8000);
                            log("Server socket created");
                            log("Waiting for agents to connect...");


                            clients.remove(key);

                            byte[] byteResponse = "8000".getBytes("utf8");

                            DatagramPacket response
                                    = new DatagramPacket(
                                    byteResponse, byteResponse.length, address, port);

                            datagramSocket.send(response);

                            Socket clientSocket = serverSocket.accept();
                            log("Agent connected: " + getClientInfo(clientSocket));

                            InputStream sis = clientSocket.getInputStream();
                            OutputStream sos = clientSocket.getOutputStream();
                            InputStreamReader sisr = new InputStreamReader(sis);
                            OutputStreamWriter sosr = new OutputStreamWriter(sos);
                            BufferedReader br = new BufferedReader(sisr);
                            BufferedWriter bw = new BufferedWriter(sosr);

                            log("Agent message: "+br.readLine());
                            bw.write("Got message");
                            bw.newLine();
                            bw.flush();


                            log("Client socket closing");
                            clientSocket.close();
                            serverSocket.close();
                            log("Client socket close");

                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    public static void main(String[] args) throws Exception {


        //Otwarcie gniazda z okreslonym portem

        for (Integer port : Config.PORTS) {

            ListeningPort listeningPort = new ListeningPort(port);
            threads.submit(listeningPort);

        }



    }

    private static void log(String message) {
        System.out.println("SERVER: " + message);
        System.out.flush();
    }

    private static String getClientInfo(Socket clientSocket) {
        String clientIP = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();
        return "["+clientIP+"]:" + clientPort;
    }
}
