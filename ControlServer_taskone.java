/**
 * This program runs as a server and controls the force to be applied to balance the Inverted Pendulum system running on the clients.
 */
import java.io.*;
import java.net.*;
import java.util.*;

import javax.lang.model.util.ElementScanner6;

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
    boolean alt = false;
    boolean stopped = false;
    boolean once = false;
    int count = 0;

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
                  
                  System.out.println("server < pole["+i+"]: Angle:"+angle+"  AngleDot:"
                      +angleDot+"  Pos:"+pos+"  PosDot:"+posDot);
                  if (i == 0)
                    actions[i] = calculate_action(angle, angleDot, pos, posDot);
                  else
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
    double calculate_action(double angle, double angleDot, double pos, double posDot) {
        double action = 0;
         //if (angle > 0 && angleDiff < 0) {
         if (angle > 0) {
             if (angle > 65 * 0.01745) {
                 action = 9;
             } else if (angle > 60 * 0.01745) {
                 action = 7;
             } else if (angle > 50 * 0.01745) {
                 action = 6;
             } else if (angle > 30 * 0.01745) {
                 action = 4;
             } else if (angle > 20 * 0.01745) {
                 action = 2;
             } else if (angle > 10 * 0.01745) {
                 action = 0.5;
             } else if(angle >5*0.01745){
                 action = 0.2;
             } else if(angle >2*0.01745){
                 action = 0.1;
             } else {
                 action = 0.05;
             }
         } else if (angle < 0) {
             if (angle < -65 * 0.01745) {
                 action = -9;
             } else if (angle < -60 * 0.01745) {
                 action = -7;
             } else if (angle < -50 * 0.01745) {
                 action = -6;
             } else if (angle < -30 * 0.01745) {
                 action = -4;
             } else if (angle < -20 * 0.01745) {
                 action = -2;
             } else if (angle < -10 * 0.01745) {
                 action = -0.5;
             } else if(angle <-5*0.01745){
                 action = -0.2;
             } else if(angle <-2*0.01745){
                 action = -0.1;
             } else {
                 action = -0.05;
             } 
          } else {
              action = 0;
          }
          if (angleDot > 0) {
              if (angleDot > 65 * 0.01745) {
                  action += 5;
              } else if (angleDot > 60 * 0.01745) {
                  action += 4;
              } else if (angleDot > 50 * 0.01745) {
                  action += 3;
              } else if (angleDot > 30 * 0.01745) {
                  action += 2;
              } else if (angleDot > 20 * 0.01745) {
                  action += .5;
              } else if (angleDot > 10 * 0.01745) {
                  action += 0.25;
              } else if(angleDot > 5*0.01745){
                  action += 0.1;
              } else if(angleDot > 2*0.01745){
                  action += 0.05;
              } else {
                  action += 0;
              } 
          } else if (angleDot < 0) {
              if (angleDot < -65 * 0.01745) {
                  action += -5;
              } else if (angleDot < -60 * 0.01745) {
                  action += -4;
              } else if (angleDot < -50 * 0.01745) {
                  action += -3;
              } else if (angleDot < -30 * 0.01745) {
                  action += -2;
              } else if (angleDot < -20 * 0.01745) {
                  action += -.5;
              } else if (angleDot < -10 * 0.01745) {
                  action += -0.25;
              } else if(angleDot <-5*0.01745){
                  action += -0.1;
              } else if(angleDot <-2*0.01745){
                  action += -0.05;
              } else {
                  action += 0;
              } 
          } else {
             action = 0;
         }
         return action;
     }
    double calculate_action2(double angle, double angleDot, double pos, double posDot, double target_pos) {
        double action = 0;
        //double target_pos = 2; // change this for cart 2
        double mid = (start_pos + target_pos)/4;
        double dist = target_pos - start_pos;
        if (pos < mid) {
            action -= 0.09;
        }
        if (pos >= start_pos + (dist * .97)){
            System.out.println("Stablization activate");

            //Apply Stabilization algorithm
            if (angle > 0) {
                if (angle > 0.3) {
                    action = .5;
                }
                else if(angle > .2) {
                    action = .45;
                }
                else if (angle > .1) {
                    action = 0.3;
                }
                else if (angle > .05) {
                    action = 0.2;
                } else if(angle >0.01){
                    action = 0.1;
                } else if(angle > 0.005){
                    action = 0.05;
                } else {
                    action = 0.01;
                }
            } else if (angle < 0) {
                if (angle < -0.4) {
                    action = -.6;
                }
                else if (angle < -0.3) {
                    action = -.5;
                }
                else if (angle < -0.2) {
                    action = -.4;
                } else if(angle <-0.1){
                    action = -0.35;
                } else if(angle <-0.05){
                    action = -0.2;
                } else if (angle <-0.01) {
                    action = -0.1;
                }
                else if (angle < -0.005) {
                    action = -.05;
                }
                else {                    
                    action = -0.01;
                } 
             } else {
                 action = 0;
             }
             if (once == false && Math.abs(angle) < 0.05 && Math.abs(posDot) > 0.06) {
                System.out.println("Run!");
                count++;
                action += 8.85;
                once = true;

            }
             if (angleDot > 0) {
                 if(angleDot > 0.5){
                     action += 0.3;
                 } else if(angleDot > 0.2){
                     action += 0.2;
                 } else if(angleDot > 0.1) {
                     action += 0.1;
                 } 
                 else {
                     action += 0.05;
                 } 
             } else if (angleDot < 0) {
                 if (angleDot < -2) {
                     action -= 1;
                 }
                 else if (angle < -1) {
                     action -= 0.7;
                 }
                 else if (angleDot < -0.7) {
                     action -= 0.8;
                 }
                 else if (angleDot < -0.6) {
                     action -= 0.5;
                 }
                 else if (angleDot < -0.5){
                    action -= 0.35;
                 }
                 else if (angleDot < -0.2) {
                     action -= 0.2;
                 }
                 else if (angleDot < -0.05) {
                     action += -0.1;
                 }
                 else if(angleDot <-0.03){
                     action += -0.8;
                 } else if(angleDot <-0.01){
                     action += -0.05;
                 } else {
                     action += -0.04;
                 } 
             } else {
                action = 0;
            }
            if (angle > 0) {
                if (posDot > 0) {
                    //action += posDot*.25;
                }
                else if (posDot < 0) {
                    //action -= posDot*.25;
                }
                if (pos > target_pos) {
                    //action += pos*.15;
                }
            }
            if (posDot < 0.1) {
                //action -= 0.03;
            }
            if (pos >= target_pos && Math.abs(posDot) < 0.2) {
                System.out.println("stable?");
                if (angle > 0) {
                    if (angle > 1) {
                        action += 1;
                    }
                    else if (angle > 0.8) {
                        action += 0.9;
                    }
                    else if (angle > 0.6) {
                        action += 0.8;
                    }
                    else if (angle > 0.4) {
                        action += .6;
                    }
                    else if (angle >0.3){
                        action += 0.3;
                    }
                    else if (angle > 0.2) {
                        action += 0.2;
                    }
                    else if (angle > 0.1) {
                        action += 0.1;
                    }
                    else {
                        action += 0.05;
                    }
                    
                }
                else if (angle < 0) {
                    if (angle < -1) {
                        action -= 1;
                    }
                    else if (angle < -0.8) {
                        action -= 0.7;
                    }
                    else if (angle < -0.6) {
                        action -= 0.6;
                    }
                    else if (angle < -0.4) {
                        action -= .4;
                    }
                    else if(angle < -0.3) {
                        action -= .3;
                    }
                    else if (angle < -0.2) {
                        action -= 0.2;
                    }
                    else if(angle < -0.1) {
                        action -= 0.1;
                    }
                    else {
                        action -= 0.05;
                    }
  
                }
                if (angleDot > 0) {
                    if (angleDot > 1) {
                        action += .4;
                    }
                    else if (angleDot > 0.5) {
                        action += 0.3;
                    }
                    else if (angleDot >0.3) {
                        action += 0.2;
                    }
                    else if (angleDot > 0.1) {
                        action += 0.07;
                    }
                    else {
                        action += 0.01;
                    }
                }
                else if(angleDot < 0) {
                    if (angleDot < -1) {
                        action -= .4;
                    }
                    else if (angleDot < -0.5) {
                        action -= 0.3;
                    }
                    else if (angleDot < -0.3) {
                        action -= 0.2;
                    }
                    else if(angleDot < -0.1) {
                        action -= 0.07;
                    }
                    else {
                        action -= 0.01;
                    }

                }
                if (action == 0){
                    action += angle;
                }
                action += posDot*.35;
                if (posDot > 0.2) {
                    action += 0.2;
                }
                if (posDot < -0.2) {
                    action -= 0.2;
                }
                
            }
        }
        else {     
            if (angle > 0) {
                if (angle > 1) {
                    action += 1;
                }
                else if (angle > 0.8) {
                    action += 0.9;
                }
                else if (angle > 0.6) {
                    action += 0.8;
                }
                else if (angle > 0.4) {
                    action += .6;
                }
                else if (angle > 0.2) {
                    action += 0.5;
                }
                else {
                    action += 0.35;
                }
            }
            else if (angle < 0) {
                if (angle < -1) {
                    action -= 1;
                }
                else if (angle < -0.8) {
                    action -= 0.8;
                }
                else if (angle < -0.6) {
                    action -= 0.6;
                }
                else if (angle < -0.4) {
                    action -= .4;
                }
                else if (angle < -0.2) {
                    action -= 0.2;
                }
                else if(angle < -0.1) {
                    action -= 0.1;
                }
                else {
                    action -= 0.05;
                }
            }
            if (angleDot > 0) {
                if (angleDot > 1) {
                    action += 1;
                }
                else if(angleDot > 0.5) {
                    action += 0.5;
                }
            }
            else if(angleDot < 0) {
                if (angleDot < -1) {
                    action -= 1;
                }
                else {
                    action -= 0.5;
                }
            }
         }
        return action;
    }
    // Calculate the actions to be applied to the inverted pendulum from the
    // sensing data.
    // TODO: Current implementation assumes that each pole is controlled
    // independently. The interface needs to be changed if the control of one
    // pendulum needs sensing data from other pendulums.
    /*double calculate_action(double angle, double angleDot, double pos, double posDot) {
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
                 action = 6;
             } else if (angle > 30 * 0.01745) {
                 action = 5;
             } else if (angle > 20 * 0.01745) {
                 action = 3.5;
             } else if (angle > 10 * 0.01745) {
                 action = 2;
             } else if(angle >5*0.01745){
                 action = 1;
             } else if(angle >2*0.01745){
                 action = 0.75;
             } else {
                 action = 0.5;
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
                 action = -3;
             } else if (angle < -10 * 0.01745) {
                 action = -2;
             } else if(angle <-5*0.01745){
                 action = -1;
             } else if(angle <-2*0.01745){
                 action = -.75;
             } else {
                 action = -0.5;
             } 
          } else {
              action = 0;
          }
          if (angleDot > 0) {
              if (angleDot > 65 * 0.01745) {
                  action += 5;
              } else if (angleDot > 60 * 0.01745) {
                  action += 4;
              } else if (angleDot > 50 * 0.01745) {
                  action += 3;
              } else if (angleDot > 30 * 0.01745) {
                  action += 3;
              } else if (angleDot > 20 * 0.01745) {
                  action += 2;
              } else if (angleDot > 10 * 0.01745) {
                  action += 1;
              } else if(angleDot > 5*0.01745){
                  action += .6;
              } else if(angleDot > 2*0.01745){
                  action += 0.5;
              } else {
                  action += 0.2;
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
                  action += -1;
              } else if(angleDot <-5*0.01745){
                  action += -.5;
              } else if(angleDot <-2*0.01745){
                  action += -0.25;
              } else {
                  action += -0.1;
              } 
          } else {
             action = 0;
         }
         //Michael: Probably should do preventative measures with the velocity
         /*if (posDot < 0) {
             if (posDot < -0.5) {
                 action += .5;
             }
         } else if (posDot > 0) {
             if (posDot > 0.5) {
                 action += -.5;
             }
        }
        int target_pos = 2;
        int start_pos = -2;
        System.out.println("***pos: " + pos);
        if ( pos < (target_pos+start_pos)/2 -.25){//first_iteration ) {
            action -= (target_pos);
            System.out.println("neg");
            first_iteration = false;
        }

        if ( pos < target_pos ) 
            if (angleDot > -2 * 0.01745 )
                action += 1;
        if ( pos >= target_pos-1 && hit_pos == false){
            hit_pos = true;
            action += target_pos;
        }
        if ( posDot > 0 && angle < 0 && hit_pos){
            System.out.println("**hit here");
            action = -3;            
        }
        if (posDot > 0 && Math.abs(angle) <= 0.02 && pos > target_pos-.5) {
            action *= .66;
            //action = action / 2;
            //System.out.println("SLOWDOWN ACTIVATED");
        }
        if (pos >= target_pos-.3) { // begin stabilzing
            System.out.println("reached 2");
            System.out.println("ANGLE IS " + angle);
            System.out.println("ANGLE DOT IS " + angleDot);
            System.out.println("POS DOT IS " + posDot);
            action = 0;
            if (angle > 0) {
                if (angle > .1/*20 * 0.01745) {
                    action = 1.5;
                } else if (angle > .07) {
                    action = 1;
                } else if(angle >.05){
                    action = .76;
                } else if(angle >.04){
                    action = .5;
                } else if(angle > .03){
                    action = 0.4;
                } else if(angle > .02){
                    action = 0.3;
                }
                else if(angle > .01){
                    action = 0.2;
                }
                else {
                    action = 0.1;
                }
            } else if (angle < 0) {
                if (angle < -.1) {
                    action = -1.5;
                } else if (angle < -.07) {
                    action = -1;
                } else if(angle <-.06){
                    action = -.75;
                } else if(angle <-.05){
                    action = -.5;
                } else if (angle <-.04){
                    action = -.4;
                } else if (angle <-.03){
                    action = -.3;
                } else if (angle <-.02){
                    action = -.2;
                }  
                else {
                    action = -0.1;
                } 
             } else {
                 action = 0;
             }
             if (angleDot > 0) {
                 if (angleDot > 65 * 0.01745) {
                     action += 1;
                 } else if (angleDot > 60 * 0.01745) {
                     action += .9;
                 } else if (angleDot > 50 * 0.01745) {
                     action += .8;
                 } else if (angleDot > 30 * 0.01745) {
                     action += .7;
                 } else if (angleDot > 20 * 0.01745) {
                     action += .5;
                 } else if (angleDot > 10 * 0.01745) {
                     action += .4;
                 } else if(angleDot > 5*0.01745){
                     action += .3;
                 } else if(angleDot > 2*0.01745){
                     action += .2;
                 } else {
                     action += .1;
                 } 
             } else if (angleDot < 0) {
                 if (angleDot < -65 * 0.01745) {
                     action += -5;
                 } else if (angleDot < -60 * 0.01745) {
                     action += -4;
                 } else if (angleDot < -50 * 0.01745) {
                     action += -3;
                 } else if (angleDot < -30 * 0.01745) {
                     action += -.7;
                 } else if (angleDot < -20 * 0.01745) {
                     action += -.5;
                 } else if (angleDot < -10 * 0.01745) {
                     action += -.4;
                 } else if(angleDot <-5*0.01745){
                     action += -.3;
                 } else if(angleDot <-2*0.01745){
                     action += -0.2;
                 } else {
                     action += -0.1;
                 } 
             } 
            
            if (posDot > 0) {
                if (posDot > 1) {
                    action += -1;
                }
                else if (posDot > .6) {
                    action += -.5;
                }
                else if (posDot > .3) {
                    action += -.2;
                }
            }
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
        }  

        return action;
     
    }*/

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
