package com.fruits.dht;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class FindNodeThread implements Runnable {
    private final int FIND_NODE_THREAD_TIME_OUT = 5 * 60 * 1000; // ms

    private final FindNodeTask findNodeTask;
    private final DHTManager dhtManager;

    public FindNodeThread(FindNodeTask findNodeTask, DHTManager dhtManager) {
        this.findNodeTask = findNodeTask;
        this.dhtManager = dhtManager;
    }

    public void run() {
        long endTime = System.currentTimeMillis() + FIND_NODE_THREAD_TIME_OUT;
        String transactionId = findNodeTask.getTransactionId();
        String targetNodeId = findNodeTask.getTargetNodeId();

        for(;;) {
            if(Thread.interrupted())
                break;

            if(System.currentTimeMillis() > endTime)
                break;

            try {
                // waiting for 5 ms if there is no object.
                // so we could check whether it is timeout.
                Node node = findNodeTask.getQueryingNodes().poll(5, TimeUnit.MILLISECONDS);

                if(node != null) {
                    String nodeId = node.getId();

                    if(nodeId != null && nodeId.equals(targetNodeId)) {
                        // found the target node
                        // TODO: how to return the target node?
                        // TODO: how to clear the resource of this find_node request?

                        dhtManager.getQueries().remove(transactionId);
                        dhtManager.removeFindNodeTask(transactionId);
                        break;
                    }

                    // put will block till there is space.
                    findNodeTask.getQueriedNodes().put(node);

                    // emit another findNode request.
                    dhtManager.putQuery(transactionId, findNodeTask.getFindNodeQuery());
                    ByteBuffer bytes = findNodeTask.getFindNodeQueryBytes();
                    bytes.rewind();
                    Datagram datagram = new Datagram(node.getAddress(), bytes);
                    dhtManager.getUdpServer().addDatagramToSend(datagram);
                }
            }catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
