package raft.impl;

import raft.StateMachine;
import raft.common.Peer;
import raft.common.RDBParser;
import raft.entity.Command;
import raft.entity.LogEntry;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.logging.Logger;

public class StateMachineIMPL implements StateMachine {
    //TODO: whether we should use disk-based database instead of Redis, since everytime we read
    // data from disk we should shutdown Redis, load RDB file and restart the Redis.

    public static final java.util.logging.Logger logger = Logger.getLogger(StateMachineIMPL.class.getName());

    NodeIMPL node;
    Jedis jedis;
    public static String workspace = "./redis_raft/";

    /**
     * "./redis_raft/${port}.dump.rdb"
     */
    String rdbPath;

    //    String confPath;


    public StateMachineIMPL(NodeIMPL node) {
        this.node = node;
        jedis = node.getJedis();
        rdbPath = workspace + Peer.getIP(node.getAddr()) + ".dump.rdb";
//        confPath = workspace + Peer.getIP(node.getAddr()) + ".conf";
//        synchronized (this) {
//            File file = new File(confPath);
//            if(!file.exists()){
//                try {
//                    file.createNewFile();
//                    logger.info(String.format("File %s create success", node.getAddr()));
//                } catch (IOException e) {
//                    logger.severe(String.format("File %s create failed", node.getAddr()));
//                }
//            } else {
//                logger.info(String.format("File %s already exists", node.getAddr()));
//            }
//        }
    }

    // TODO: I don't think synchronized is needed since Redis is single-threaded.
    @Override
    public void apply(LogEntry logEntry) {
        Command command = logEntry.getCommand();
        if (command == null) {
            throw new IllegalArgumentException(logEntry + ": Command cannot be null");
        }
        String key = command.getKey();
        String value = command.getValue();
        jedis.setnx(key, value);
        // also save logs, because state machine module and log entry module share same jedis instance
        // should be optimized, e.g. use disk-based database, if data becomes huge.
        jedis.save();
    }


    //TODO: Plan (A) use third-party parsing library to parse RDB file and get value
    //      Plan (B) find out how to use a temporary Redis instance to load RDB file and get value
    @Override
    public String getVal(String key) {
        File rdbFile = new File(rdbPath);
        if (rdbFile.exists()) {
            return RDBParser.getVal(rdbFile, key);
        }
        return null;
    }

    @Override
    public void setVal(String key, String value) {

    }

    @Override
    public void delVal(String... key) {

    }
}