
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

/*
 * This is an thread that belongs in a "pool" that handles socket connection.
 * Using this architecture prevents the possibility that we overload our server with threads and 
 * also removes the overhead of spawning threads constantly.
 */


public class SocketThreadPoolHandler extends Thread
{
    private BlockingQueue<SocketConnection> m_clientSockets;
    
    private static SocketListener s_listener = null;
    
    public SocketThreadPoolHandler(SocketListener listener)
    {
        s_listener = listener;
    }
    
    
    /**
     * Add a socket to this handlers collection of sockets that it is responsible for.
     * @param clientSocket 
     */
    public void addSocket(Socket clientSocket)
    {
        SocketConnection conn = new SocketConnection(clientSocket);
        m_clientSockets.add(conn);
    }
    
    
    /**
     * Return the number of sockets this thread is currently handling.
     * @return the number of sockets
     */
    public int getSocketCount()
    {
        return m_clientSockets.size();
    }
    
    
    public void run()
    {
        while (true)
        {
            Iterator socketIterator = m_clientSockets.iterator();
            
            while (socketIterator.hasNext())
            {
                try
                {
                    // This will automatically wait/block when nothing is available.
                    SocketConnection connection = (SocketConnection)socketIterator.next();
                    HandlerLogic.handleSocket(connection);
                    
                    if (connection.isClosed())
                    {
                        socketIterator.remove();
                    }
                }
                catch (Exception e)
                {
                    System.out.println("SocketThreadPoolHandler: " + e.getMessage().toString());
                }
            }
        }
    }
}
