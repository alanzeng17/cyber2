/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class ControlServer {

    private static ServerSocket serverSocket;
    private static final int port = 25533;

    /**
     * Main method that creates new socket and PoleServer instance and runs it.
     */
    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.out.println("unable to set up port");
            System.exit(1);
        }
        System.out.println("Waiting for connection");
        do {
            Socket client = serverSocket.accept();
            System.out.println("\nnew client accepted.\n");
            PoleServer_handler handler = new PoleServer_handler(client);
        } while (true);
    }
}

/**
 * This class sends control messages to balance the pendulum on client side.
 */
class PoleServer_handler implements Runnable {
    // Set the number of poles
    private static final int NUM_POLES = 1;

    static ServerSocket providerSocket;
    Socket connection = null;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message = "abc";
    static Socket clientSocket;
    Thread t;
    int start_pos = -2;

    /**
     * Class Constructor
     */
    public PoleServer_handler(Socket socket) {
        t = new Thread(this);
        clientSocket = socket;

        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        t.start();
    }
    double angle, angleDot, pos, posDot, action = 0, i = 0;

    /**
     * This method receives the pole positions and calculates the updated value
     * and sends them across to the client.
     * It also sends the amount of force to be applied to balance the pendulum.
     * @throws ioException
     */
    void control_pendulum(ObjectOutputStream out, ObjectInputStream in) {
        try {
            while(true){
                System.out.println("-----------------");

                // read data from client
                Object obj = in.readObject();

                // Do not process string data unless it is "bye", in which case,
                // we close the server
                if(obj instanceof String){
                    System.out.println("STRING RECEIVED: "+(String) obj);
                    if(obj.equals("bye")){
                        break;
                    }
                    continue;
                }
                
                double[] data= (double[])(obj);
                assert(data.length == NUM_POLES * 4);
                double[] actions = new double[NUM_POLES];
 
                // Get sensor data of each pole and calculate the action to be
                // applied to each inverted pendulum
                // TODO: Current implementation assumes that each pole is
                // controlled independently. This part needs to be changed if
                // the control of one pendulum needs sensing data from other
                // pendulums.
                for (int i = 0; i < NUM_POLES; i++) {
                  angle = data[i*4+0];
                  angleDot = data[i*4+1];
                  pos = data[i*4+2];
                  posDot = data[i*4+3];
                  
                  System.out.println("server < pole["+i+"]: "+angle+"  "
                      +angleDot+"  "+pos+"  "+posDot);
                  actions[i] = calculate_action(angle, angleDot, pos, posDot);
                }

                sendMessage_doubleArray(actions);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (clientSocket != null) {
                System.out.println("closing down connection ...");                
                out.writeObject("bye");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            }
        } catch (IOException ioe) {
            System.out.println("unable to disconnect");
        }

        System.out.println("Session closed. Waiting for new connection...");

    }

    /**
     * This method calls the controller method to balance the pendulum.
     * @throws ioException
     */
    public void run() {

        try {
            control_pendulum(out, in);

        } catch (Exception ioException) {
            ioException.printStackTrace();
        } finally {
        }

    }

    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    // TODO: Current implementation assumes that each pole is controlled
    // independently. The interface needs to be changed if the control of one
    // pendulum needs sensing data from other pendulums.
    double calculate_action(double angle, double angleDot, double pos, double posDot) {
        double action = 0;
        boolean first_iteration = true;
        boolean hit_pos = false;
         //if (angle > 0 && angleDiff < 0) {
         if (angle > 0) {
             if (angle > 65 * 0.01745) {
                 action = 9;
             } else if (angle > 60 * 0.01745) {
                 action = 7;
             } else if (angle > 50 * 0.01745) {
                 action = 5;
             } else if (angle > 30 * 0.01745) {
                 action = 3;
             } else if (angle > 20 * 0.01745) {
                 action = 2;
             } else if (angle > 10 * 0.01745) {
                 action = 3;
             } else if(angle >5*0.01745){
                 action = .5;
             } else if(angle >2*0.01745){
                 action = 0.25;
             } else {
                 action = 0.05;
             }
         } else if (angle < 0) {
             if (angle < -65 * 0.01745) {
                 action = -7;
             } else if (angle < -60 * 0.01745) {
                 action = -6;
             } else if (angle < -50 * 0.01745) {
                 action = -5;
             } else if (angle < -30 * 0.01745) {
                 action = -4;
             } else if (angle < -20 * 0.01745) {
                 action = -4;
             } else if (angle < -10 * 0.01745) {
                 action = -4;
             } else if(angle <-5*0.01745){
                 action = -0;
             } else if(angle <-2*0.01745){
                 action = -1;
             } else {
                 action = -0;
             } 
          } else {
              action = 0;
          }
          if (angleDot > 0) {
              if (angleDot > 65 * 0.01745) {
                  action += 9;
              } else if (angleDot > 60 * 0.01745) {
                  action += 5;
              } else if (angleDot > 50 * 0.01745) {
                  action += 4;
              } else if (angleDot > 30 * 0.01745) {
                  action += 3;
              } else if (angleDot > 20 * 0.01745) {
                  action += 2;
              } else if (angleDot > 10 * 0.01745) {
                  action += 1;
              } else if(angleDot > 5*0.01745){
                  action += 0.2;
              } else if(angleDot > 2*0.01745){
                  action += 0.1;
              } else {
                  action += 0.01;
              } 
          } else if (angleDot < 0) {
              if (angleDot < -65 * 0.01745) {
                  action += -9;
              } else if (angleDot < -60 * 0.01745) {
                  action += -3;
              } else if (angleDot < -50 * 0.01745) {
                  action += -3;
              } else if (angleDot < -30 * 0.01745) {
                  action += -3;
              } else if (angleDot < -20 * 0.01745) {
                  action += -2;
              } else if (angleDot < -10 * 0.01745) {
                  action += -1.5;
              } else if(angleDot <-5*0.01745){
                  action += -1;
              } else if(angleDot <-2*0.01745){
                  action += -0.25;
              } else {
                  action += -0.1;
              } 
          } else {
             action = 0;
         }
         //Michael: Probably should do preventative measures with the velocity
         if (posDot < 0) {
             if (posDot < -0.5) {
                 action += 0.2;
             }
         } else if (posDot > 0) {
             if (posDot > 0.5) {
                 action += -0.2;
             }
        }
        int target_pos = 2;
        System.out.println("***pos: " + pos);
        if ( first_iteration ) {
            action -= target_pos;
            first_iteration = false;
        }
        if ( pos < target_pos ) 
            if (angleDot > -2 * 0.01745 )
                action += 3;
        if ( pos >= target_pos && hit_pos == false){
            hit_pos = true;
            action += 4*target_pos;
        }
        if ( posDot > 0 && angle < 0 && hit_pos){
            System.out.println("**hit here");
            action = -6;            
        }

        /*if ( hit_pos ) {
            if ( angle < 0 )
                hit_pos = false;
            action += 5;
        }*/
        //else if ( pos > targetPos )
            //action -= 1;
        /*if (pos < 0 && angle < 10 * 0.01745)
            action -= 6;
        else if (pos == 2) {
            action = 0;
        }
        else if (pos >= 1 && pos < 2) {
            action += 3.5;
        } else if (pos > 2) {
            //action -= 5;
            action = 0;
        }  */ 
        System.out.println("***action: " + action);
        return action;
     
   }

    /**
     * This method sends the Double message on the object output stream.
     * @throws ioException
     */
    void sendMessage_double(double msg) {
        try {
            out.writeDouble(msg);
            out.flush();
            System.out.println("server>" + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * This method sends the Double message on the object output stream.
     */
    void sendMessage_doubleArray(double[] data) {
        try {
            out.writeObject(data);
            out.flush();
            
            System.out.print("server> ");
            for(int i=0; i< data.length; i++){
                System.out.print(data[i] + "  ");
            }
            System.out.println();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }


}
