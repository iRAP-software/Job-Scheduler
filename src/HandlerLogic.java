/*
 * Because we have two possible threading types (one thread per socket or thread pools) we have all
 * the logic here that the objects share. Thus if we ever want to update the logic we only have
 * to change it here.
 */


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Iterator;


public class HandlerLogic 
{
    /**
     * This is the "main" / entry function (hence it is the only public one). Threads should call
     * this in order to handle their socket connections.
     * @param clientSocket 
     */
    public static void handleSocket(SocketConnection clientSocket)
    {
        if (clientSocket.isMessageWaiting())
        {
            String clientMsg = clientSocket.readMessage();
            processMessage(clientMsg, clientSocket);
        }
    }
    
    
    /**
     * Send a message on the socket connection.
     * @param clientSocket
     * @param responseString 
     */
    private static void sendMessage(SocketConnection clientSocket, String responseString)
    {
        clientSocket.sendMessage(responseString);
    }
    
    
    /**
     * Handle the request that came in. This can be thought of the router or handler.
     * @param String clientMsg - the message that was passed to us.
     * @return JsonObject
     */
    private static void processMessage(String clientMsg, SocketConnection clientSocket)
    {
        JsonObject response = new JsonObject();
        JsonObject cargo = null;
        
        // Convert the message which is a json string into a json object (google gson)
        JsonParser parser = new JsonParser();
        JsonObject clientMsgJson = (JsonObject)parser.parse(clientMsg);
        
        String action = "";
        
        if (clientMsgJson.has("action"))
        {
            action = clientMsgJson.get("action").getAsString();            
            clientMsgJson.remove("action");
            
            try
            {
                switch (action)
                {
                    case "add_task":
                    {
                        cargo = handleAddTask(clientMsgJson);
                    }
                    break;

                    case "get_task":
                    {
                        cargo = handleGetTask(clientMsgJson);
                    }
                    break;

                    case "complete_task":
                    {
                        handleCompleteTask(clientMsgJson);
                    }
                    break;

                    case "get_info":
                    {
                        cargo = handleGetInfo(clientMsgJson);
                    }
                    break;

                    case "reject_task":
                    {
                        handleRejectTask(clientMsgJson);
                    }
                    break;
                    
                    case "close":
                    {
                        
                    }
                    break;

                    default:
                    {
                        throw new Exception("Unrecognized action specified: " + action);
                    }
                }
                
                addToJson(response, "result", "success");
            }
            catch (Exception e)
            {
                // Any of the handlers can throw an exception.
                System.out.println("Building error response for client...");
                response = addToJson(response, "result", "error");
                String errorMessage = e.toString();
                System.out.println("error message was: " + e.toString());
                response = addToJson(response, "message", errorMessage);
                System.out.println("Error response has been generated");
            }
        }
        else
        {
            // Any of the handlers can throw an exception.
            System.out.println("Client failed to provide an action in: [" + clientMsg + "]");
            response = addToJson(response, "result", "error");
            response = addToJson(response, "message", "no action specified");
        }
        
        
        // If the client requested us to close the connection, then close it.
        if (action.equals("close"))
        {
            clientSocket.close();
        }
        else
        {
            String cargoString = "";
        
            if (cargo != null)
            {
                response.add("cargo", cargo);
            }
            else
            {
                // Still want to send a cargo element, but with nothing in it.
                response.add("cargo", new JsonPrimitive(""));
            }

            Gson gson = new Gson();
            String responseString = gson.toJson(response);
            clientSocket.sendMessage(responseString);
        }
    }
    
    
    /**
     * Helper function to just keep adding name/value pairs to a json object
     * @param obj - the object we are adding to
     * @param name - the name part of a name/value pair
     * @param value - the value of a name/value pair.
     * @return JsonObject
     */
    private static JsonObject addToJson(JsonObject obj, String name, String value)
    {
        obj.add(name, new JsonPrimitive(value));
        return obj;
    }
    
    
    /**
     * Handle a request to add a task to the scheduler.
     * @param clientMessage - the JSON object that represents the request that was sent to us
     * @return
     * @throws Exception 
     */
    private static JsonObject handleAddTask(JsonObject clientMessage) throws Exception
    {
        // This forms part of the response.
        JsonObject cargo = new JsonObject();
        
        
        if (!clientMessage.has("task_name"))
        {
            throw new Exception("Missing required parameter [task_name]");
        }
        
        String taskName = clientMessage.get("task_name").getAsString();
                
        ArrayList<Integer> dependencies = new ArrayList<>();
        
        if (clientMessage.has("dependencies"))
        {
            JsonArray dependenciesRaw = clientMessage.get("dependencies").getAsJsonArray();
            Iterator dependencyIterator = dependenciesRaw.iterator();
            
            while (dependencyIterator.hasNext())
            {
                Integer dependency = Integer.parseInt(dependencyIterator.next().toString());
                dependencies.add(dependency);
            }
        }
        
 
        String extraInfo = "";
        if (clientMessage.has("extra_info"))
        {
            Gson gson = new Gson();
            extraInfo = gson.toJson(clientMessage.get("extra_info"));
        }
        
        int priority = Task.DEFAULT_PRIORITY;
        
        if (clientMessage.has("priority"))
        {
            priority = Integer.parseInt(clientMessage.get("priority").getAsString());
        }
        
        String group = null;
        
        if (clientMessage.has("group"))
        {
            group = clientMessage.get("group").getAsString();
        }
        
        Scheduler scheduler = Scheduler.getInstance();        
        Integer newTaskId = scheduler.addTask(taskName, dependencies, extraInfo, priority, group);
        cargo.add("task_id", new JsonPrimitive(newTaskId));
        
        return cargo;
    }
    
    
    /**
     * Handler for the "get_task" request
     * @param clientMessage - the JSON object that represents the request that was sent to us
     * @return 
     */
    private static JsonObject handleGetTask(JsonObject clientMessage) throws Exception
    {
        // This forms part of the response.
        JsonObject cargo = new JsonObject();
        Scheduler scheduler = Scheduler.getInstance();
        Task taskToDo = scheduler.getAvailableTask();        
        cargo.add("task", taskToDo.jsonSerialize());        
        return cargo;
    }
    
    
    /**
     * Handle the users request to mark a task as having been completed.
     * @param clientMessage - the JSON object that represents the request that was sent to us
     * @return void
     */
    private static void handleCompleteTask(JsonObject clientMessage) throws Exception
    {
        if (!clientMessage.has("task_id"))
        {
            throw new Exception("Missing required parameter [task_id]");
        }
        
        if (!clientMessage.has("lock"))
        {
            throw new Exception("Missing required parameter [lock]"); 
        }
        
        Integer task_id = clientMessage.get("task_id").getAsInt();
        String lock     = clientMessage.get("lock").getAsString();
                
        /* @var $scheduler Scheduler */
        Scheduler scheduler = Scheduler.getInstance();
        scheduler.completeTask(task_id, lock);        
    }
    
    
    /**
     * 
     * @param clientMessage
     * @return 
     */
    private static JsonObject handleGetInfo(JsonObject clientMessage)
    {        
        Scheduler scheduler = Scheduler.getInstance();
        JsonObject cargo = scheduler.getInfo();

        return cargo;
    }
    
    
    
    /**
     * Handle the request to reject a task. Rejecting a task results in the task being unlocked and
     * marked available for other tasks. Be careful that you don't use this request when you 
     * actually want to remove a task instead.
     * @param clientMessage - the request object that should contain the task id and lock
     * @return 
     */
    private static void handleRejectTask(JsonObject clientMessage) throws Exception
    {
        // This forms part of the response.        
            
        if (!clientMessage.has("task_id"))
        {
            throw new Exception("Missing required parameter [task_id]");
        }
        
        if (!clientMessage.has("lock"))
        {
            throw new Exception("Missing required parameter [lock]"); 
        }
        
        Integer task_id = clientMessage.get("task_id").getAsInt();
        String lock     = clientMessage.get("lock").getAsString();
                
        /* @var $scheduler Scheduler */
        Scheduler scheduler = Scheduler.getInstance();
        
        // This will throw the appropriate exception if fails so dont need to build response here.
        scheduler.rejectTask(task_id, lock);
    }
    
    
    /**
     * Remove a task from the scheduler. This will remove the task if it exists in any of the
     * queues etc. This WILL NOT remove any tasks that are dependent upon it. Thus one may need to
     * make sure to remove those tasks first or remove a group instead.
     * @param clientMessage
     * @throws Exception if the task could not be removed.
     */
    private static void handleRemoveTask(JsonObject clientMessage) throws Exception
    {        
        Scheduler scheduler = Scheduler.getInstance();
        
        if (!clientMessage.has("task_id"))
        {
            throw new Exception("Missing required parameter [task_id]");
        }
        
        Integer task_id = clientMessage.get("task_id").getAsInt();
        String lock     = clientMessage.get("lock").getAsString();
                        
        // This will throw the appropriate exception if fails so dont need to build response here.
        scheduler.removeTask(task_id);
    }
    
    
    /**
     * Remove a task from the scheduler. This will remove the task if it exists in any of the
     * queues etc. This WILL NOT remove any tasks that are dependent upon it. Thus one may need to
     * make sure to remove those tasks first or remove a group instead.
     * @param clientMessage
     * @return 
     */
    private static JsonObject handleRemoveGroup(JsonObject clientMessage)
    {
        // This forms part of the response.
        JsonObject cargo = new JsonObject();
        
        return cargo;
    }
}
