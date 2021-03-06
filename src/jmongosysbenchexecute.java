//import com.mongodb.Mongo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

public class jmongosysbenchexecute {
    public static AtomicLong globalInserts = new AtomicLong(0);
    public static AtomicLong globalDeletes = new AtomicLong(0);
    public static AtomicLong globalUpdates = new AtomicLong(0);
    public static AtomicLong globalPointQueries = new AtomicLong(0);
    public static AtomicLong globalRangeQueries = new AtomicLong(0);
    public static AtomicLong globalSysbenchTransactions = new AtomicLong(0);
    public static AtomicLong globalWriterThreads = new AtomicLong(0);

    public static Writer writer = null;
    public static boolean outputHeader = true;

    public static int numCollections;
    public static String dbName;
    public static int writerThreads;
    public static int paddingSize;
    public static Integer numMaxInserts;
    public static long secondsPerFeedback;
    public static String logFileName;
    public static String indexTechnology;
    public static String autoCommit;
    public static int runSeconds;
    public static String myWriteConcern;
    public static Integer maxTPS;
    public static Integer maxThreadTPS;
    public static String serverName;
    public static int serverPort;
    public static String userName;
    public static String passWord;

    public static int oltpRangeSize;
    public static int oltpPointSelects;
    public static int oltpSimpleRanges;
    public static int oltpSumRanges;
    public static int oltpOrderRanges;
    public static int oltpDistinctRanges;
    public static int oltpIndexUpdates;
    public static int oltpNonIndexUpdates;
    public static int oltpInserts;

    public static boolean bIsTokuMX = false;

    public static int allDone = 0;

    public static long rngSeed = 0;
    
    public jmongosysbenchexecute() {
    }

    public static void main (String[] args) throws Exception {
        if (args.length != 25) {
            logMe("*** ERROR : CONFIGURATION ISSUE ***");
            logMe("jsysbenchexecute [number of collections] [database name] [number of writer threads] [size of additional padding] [documents per collection] [seconds feedback] "+
                                   "[log file name] [auto commit Y/N] [runtime (seconds)] [range size] [point selects] "+
                                   "[simple ranges] [sum ranges] [order ranges] [distinct ranges] [index updates] [non index updates] [inserts] [writeconcern] "+
                                   "[max tps] [server] [port] [seed] [username] [password]");
            System.exit(1);
        }
        
        numCollections = Integer.valueOf(args[0]);
        dbName = args[1];
        writerThreads = Integer.valueOf(args[2]);
        paddingSize = Integer.valueOf(args[3]);
        numMaxInserts = Integer.valueOf(args[4]);
        secondsPerFeedback = Long.valueOf(args[5]);
        logFileName = args[6];
        autoCommit = args[7];
        runSeconds = Integer.valueOf(args[8]);
        oltpRangeSize = Integer.valueOf(args[9]);
        oltpPointSelects = Integer.valueOf(args[10]);
        oltpSimpleRanges = Integer.valueOf(args[11]);
        oltpSumRanges = Integer.valueOf(args[12]);
        oltpOrderRanges = Integer.valueOf(args[13]);
        oltpDistinctRanges = Integer.valueOf(args[14]);
        oltpIndexUpdates = Integer.valueOf(args[15]);
        oltpNonIndexUpdates = Integer.valueOf(args[16]);
        oltpInserts = Integer.valueOf(args[17]);
        myWriteConcern = args[18];
        maxTPS = Integer.valueOf(args[19]);
        serverName = args[20];
        serverPort = Integer.valueOf(args[21]);
        rngSeed = Long.valueOf(args[22]);
        userName = args[23];
        passWord = args[24];

        maxThreadTPS = (maxTPS / writerThreads) + 1;

        WriteConcern myWC = new WriteConcern();
        if (myWriteConcern.toLowerCase().equals("fsync_safe")) {
            myWC = WriteConcern.FSYNC_SAFE;
        }
        else if ((myWriteConcern.toLowerCase().equals("none"))) {
            myWC = WriteConcern.NONE;
        }
        else if ((myWriteConcern.toLowerCase().equals("normal"))) {
            myWC = WriteConcern.NORMAL;
        }
        else if ((myWriteConcern.toLowerCase().equals("replicas_safe"))) {
            myWC = WriteConcern.REPLICAS_SAFE;
        }
        else if ((myWriteConcern.toLowerCase().equals("safe"))) {
            myWC = WriteConcern.SAFE;
        }
        else {
            logMe("*** ERROR : WRITE CONCERN ISSUE ***");
            logMe("  write concern %s is not supported",myWriteConcern);
            System.exit(1);
        }

        logMe("Application Parameters");
        logMe("-------------------------------------------------------------------------------------------------");
        logMe("  collections              = %d",numCollections);
        logMe("  database name            = %s",dbName);
        logMe("  writer threads           = %d",writerThreads);
        logMe("  additional padding(bytes)= %d",paddingSize);
        logMe("  documents per collection = %,d",numMaxInserts);
        logMe("  feedback seconds         = %,d",secondsPerFeedback);
        logMe("  log file                 = %s",logFileName);
        logMe("  auto commit              = %s",autoCommit);
        logMe("  run seconds              = %d",runSeconds);
        logMe("  oltp range size          = %d",oltpRangeSize);
        logMe("  oltp point selects       = %d",oltpPointSelects);
        logMe("  oltp simple ranges       = %d",oltpSimpleRanges);
        logMe("  oltp sum ranges          = %d",oltpSumRanges);
        logMe("  oltp order ranges        = %d",oltpOrderRanges);
        logMe("  oltp distinct ranges     = %d",oltpDistinctRanges);
        logMe("  oltp index updates       = %d",oltpIndexUpdates);
        logMe("  oltp non index updates   = %d",oltpNonIndexUpdates);
        logMe("  oltp inserts             = %d",oltpInserts);
        logMe("  write concern            = %s",myWriteConcern);
        logMe("  maximum tps (global)     = %d",maxTPS);
        logMe("  maximum tps (per thread) = %d",maxThreadTPS);
        logMe("  Server:Port = %s:%d",serverName,serverPort);
        logMe("  seed                     = %d",rngSeed);
        logMe("  userName                 = %s",userName);

        MongoClientOptions clientOptions = new MongoClientOptions.Builder().connectionsPerHost(2048).socketTimeout(60000).writeConcern(myWC).build();
        ServerAddress srvrAdd = new ServerAddress(serverName,serverPort);

        // Credential login is optional.
        MongoClient m;
        if (userName.isEmpty() || userName.equalsIgnoreCase("none")) {
            m = new MongoClient(srvrAdd);
        } else {
            MongoCredential credential = MongoCredential.createCredential(userName, dbName, passWord.toCharArray());
            m = new MongoClient(srvrAdd, Arrays.asList(credential));
        }

        logMe("mongoOptions | " + m.getMongoOptions().toString());
        logMe("mongoWriteConcern | " + m.getWriteConcern().toString());

        DB db = m.getDB(dbName);

        // determine server type : mongo or tokumx
        DBObject checkServerCmd = new BasicDBObject();
        CommandResult commandResult = db.command("buildInfo");

        // check if tokumxVersion exists, otherwise assume mongo
        if (commandResult.toString().contains("tokumxVersion")) {
            indexTechnology = "tokumx";
        }
        else
        {
            indexTechnology = "mongo";
        }

        logMe("  index technology         = %s",indexTechnology);
        logMe("-------------------------------------------------------------------------------------------------");

        try {
            writer = new BufferedWriter(new FileWriter(new File(logFileName)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ((!indexTechnology.toLowerCase().equals("tokumx")) && (!indexTechnology.toLowerCase().equals("mongo"))) {
            // unknown index technology, abort
            logMe(" *** Unknown Indexing Technology %s, shutting down",indexTechnology);
            System.exit(1);
        }

        if (indexTechnology.toLowerCase().equals("tokumx")) {
            bIsTokuMX = true;
        }

        jmongosysbenchexecute t = new jmongosysbenchexecute();

        Thread[] tWriterThreads = new Thread[writerThreads];

        for (int i=0; i<writerThreads; i++) {
            tWriterThreads[i] = new Thread(t.new MyWriter(writerThreads, i, numMaxInserts, db, numCollections, rngSeed));
            tWriterThreads[i].start();
        }

        Thread reporterThread = new Thread(t.new MyReporter());
        reporterThread.start();
        reporterThread.join();

        // wait for writer threads to terminate
        for (int i=0; i<writerThreads; i++) {
            if (tWriterThreads[i].isAlive())
                tWriterThreads[i].join();
        }

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        m.close();

        logMe("Done!");
    }

    class MyWriter implements Runnable {
        int threadCount;
        int threadNumber;
        int numTables;
        int numMaxInserts;
        int numCollections;
        DB db;

        long numInserts = 0;
        long numDeletes = 0;
        long numUpdates = 0;
        long numPointQueries = 0;
        long numRangeQueries = 0;

        java.util.Random rand;
        
        MyWriter(int threadCount, int threadNumber, int numMaxInserts, DB db, int numCollections, long rngSeed) {
            this.threadCount = threadCount;
            this.threadNumber = threadNumber;
            this.numMaxInserts = numMaxInserts;
            this.db = db;
            this.numCollections = numCollections;
            rand = new java.util.Random((long) threadNumber + rngSeed);
        }
        public void run() {
            logMe("Writer thread %d : started",threadNumber);
            globalWriterThreads.incrementAndGet();

            long numTransactions = 0;
            long numLastTransactions = 0;
            long nextMs = System.currentTimeMillis() + 1000;

            boolean auto_commit = !autoCommit.toLowerCase().equals("n");

            // initialize char array to fill into document
            char[] pad = new char[paddingSize];
            for (int i = 0; i < paddingSize; i++) {
              pad[i] = '#';
            }
            String padStr = String.valueOf(pad);            
            
            while (allDone == 0) {
                if ((numTransactions - numLastTransactions) >= maxThreadTPS) {
                    // pause until a second has passed
                    while (System.currentTimeMillis() < nextMs) {
                        try {
                            Thread.sleep(20);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    numLastTransactions = numTransactions;
                    nextMs = System.currentTimeMillis() + 1000;
                }

                // if TokuMX, lock onto current connection (do not pool)
                if (bIsTokuMX && !auto_commit) {
                    db.requestStart();
                    db.command("beginTransaction");
                }

                String collectionName = "sbtest" + Integer.toString(rand.nextInt(numCollections)+1);
                DBCollection coll = db.getCollection(collectionName);

                try {
                    if (bIsTokuMX && !auto_commit) {
                        // make sure a connection is available, given that we are not pooling
                        db.requestEnsureConnection();
                    }

                    for (int i=1; i <= oltpPointSelects; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;

                        BasicDBObject query = new BasicDBObject("_id", startId);
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);

                        DBObject myDoc = coll.findOne(query, columns);
                        //System.out.println(myDoc);

                        globalPointQueries.incrementAndGet();
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpSimpleRanges; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);
                        DBCursor cursor = coll.find(query, columns);
                        try {
                            while(cursor.hasNext()) {
                                cursor.next();
                                //System.out.println(cursor.next());
                            }
                        } finally {
                            cursor.close();
                        }

                        globalRangeQueries.incrementAndGet();
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpSumRanges; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        // create our pipeline operations, first with the $match
                        DBObject match = new BasicDBObject("$match", new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId)));

                        // build the $projection operation
                        DBObject fields = new BasicDBObject("k", 1);
                        fields.put("_id", 0);
                        DBObject project = new BasicDBObject("$project", fields );

                        // Now the $group operation
                        DBObject groupFields = new BasicDBObject( "_id", null);
                        groupFields.put("average", new BasicDBObject( "$sum", "$k"));
                        DBObject group = new BasicDBObject("$group", groupFields);

                        // run aggregation
                        AggregationOutput output = coll.aggregate( match, project, group );

                        //System.out.println(output.getCommandResult());

                        globalRangeQueries.incrementAndGet();
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpOrderRanges; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);
                        DBCursor cursor = coll.find(query, columns).sort(new BasicDBObject("c",1));
                        try {
                            while(cursor.hasNext()) {
                                cursor.next();
                                //System.out.println(cursor.next());
                            }
                        } finally {
                            cursor.close();
                        }

                        globalRangeQueries.incrementAndGet();
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpDistinctRanges; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        int endId = startId + oltpRangeSize - 1;

                        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$gte", startId).append("$lte", endId));
                        BasicDBObject columns = new BasicDBObject("c", 1).append("_id", 0);
                        List lstDistinct = coll.distinct("c", query);

                        globalRangeQueries.incrementAndGet();
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpIndexUpdates; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        WriteResult wrUpdate = coll.update(new BasicDBObject("_id", startId), new BasicDBObject("$inc", new BasicDBObject("k",1)), false, false);
                        numTransactions += 1;
                    }
    
                    for (int i=1; i <= oltpNonIndexUpdates; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        String cVal = sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
                        WriteResult wrUpdate = coll.update(new BasicDBObject("_id", startId), new BasicDBObject("$set", new BasicDBObject("c",cVal)), false, false);
                        numTransactions += 1;
                    }

                    for (int i=1; i <= oltpInserts; i++) {
                        int startId = rand.nextInt(numMaxInserts)+1;
                        WriteResult wrRemove = coll.remove(new BasicDBObject("_id", startId));
                       
                        BasicDBObject doc = new BasicDBObject();
                        doc.put("_id",startId);
                        doc.put("k",rand.nextInt(numMaxInserts)+1);
                        String cVal = sysbenchString(rand, "###########-###########-###########-###########-###########-###########-###########-###########-###########-###########");
                        doc.put("c",cVal);

                        if (!padStr.isEmpty())
                          doc.put("pad",padStr);
                        try {
                          WriteResult wrInsert = coll.insert(doc);
                        } catch (Exception e) {
                          continue;
                        };
                        numTransactions += 1;
                    }

                    globalSysbenchTransactions.incrementAndGet();
                    //numTransactions += 1;

                } finally {
                    if (bIsTokuMX && !auto_commit) {
                        // commit the transaction and release current connection in the pool
                        db.command("commitTransaction");
                        //--db.command("rollbackTransaction")
                        db.requestDone();
                    }
                }
            }
            globalWriterThreads.decrementAndGet();
        }
    }


    public static String sysbenchString(java.util.Random rand, String thisMask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = thisMask.length() ; i < n ; i++) { 
            char c = thisMask.charAt(i); 
            if (c == '#') {
                sb.append(String.valueOf(rand.nextInt(10)));
            } else if (c == '@') {
                sb.append((char) (rand.nextInt(26) + 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    // reporting thread, outputs information to console and file
    class MyReporter implements Runnable {
        public void run()
        {
            long t0 = System.currentTimeMillis();
            long lastInserts = 0;
            long thisInserts = 0;
            long lastDeletes = 0;
            long thisDeletes = 0;
            long lastUpdates = 0;
            long thisUpdates = 0;
            long lastPointQueries = 0;
            long thisPointQueries = 0;
            long lastRangeQueries = 0;
            long thisRangeQueries = 0;
            long lastSysbenchTransactions = 0;
            long thisSysbenchTransactions = 0;
            long lastMs = t0;
            long intervalNumber = 0;
            long nextFeedbackMillis = t0 + (1000 * secondsPerFeedback * (intervalNumber + 1));
            long runEndMillis = Long.MAX_VALUE;
            if (runSeconds > 0)
                runEndMillis = t0 + (1000 * runSeconds);

            while ((System.currentTimeMillis() < runEndMillis) && (thisInserts < numMaxInserts))
            {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long now = System.currentTimeMillis();


//    public static AtomicLong globalDeletes = new AtomicLong(0);
//    public static AtomicLong globalUpdates = new AtomicLong(0);
//    public static AtomicLong globalPointQueries = new AtomicLong(0);
//    public static AtomicLong globalRangeQueries = new AtomicLong(0);


                thisInserts = globalInserts.get();
                thisSysbenchTransactions = globalSysbenchTransactions.get();

                if ((now > nextFeedbackMillis) && (secondsPerFeedback > 0))
                {
                    intervalNumber++;
                    nextFeedbackMillis = t0 + (1000 * secondsPerFeedback * (intervalNumber + 1));

                    long elapsed = now - t0;
                    long thisIntervalMs = now - lastMs;
                    long thisWriterThreads = globalWriterThreads.get();

                    long thisIntervalSysbenchTransactions = thisSysbenchTransactions - lastSysbenchTransactions;
                    double thisIntervalSysbenchTransactionsPerSecond = thisIntervalSysbenchTransactions/(double)thisIntervalMs*1000.0;
                    double thisSysbenchTransactionsPerSecond = thisSysbenchTransactions/(double)elapsed*1000.0;

                    long thisIntervalInserts = thisInserts - lastInserts;
                    double thisIntervalInsertsPerSecond = thisIntervalInserts/(double)thisIntervalMs*1000.0;
                    double thisInsertsPerSecond = thisInserts/(double)elapsed*1000.0;
                    
                    logMe("%,d seconds : cum tps=%,.2f : int tps=%,.2f : cum ips=%,.2f : int ips=%,.2f : writers=%,d", elapsed / 1000l, thisSysbenchTransactionsPerSecond, thisIntervalSysbenchTransactionsPerSecond, thisInsertsPerSecond, thisIntervalInsertsPerSecond, thisWriterThreads);
                    
                    try {
                        if (outputHeader)
                        {
                            writer.write("elap_secs\tcum_tps\tint_tps\tcum_ips\tint_ips\n");
                            outputHeader = false;
                        }

                        String statusUpdate = "";

                        statusUpdate = String.format("%d\t%.2f\t%.2f\t%.2f\t%.2f\n", elapsed / 1000l, thisSysbenchTransactionsPerSecond, thisIntervalSysbenchTransactionsPerSecond, thisInsertsPerSecond, thisIntervalInsertsPerSecond);

                        writer.write(statusUpdate);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    lastInserts = thisInserts;
                    lastSysbenchTransactions = thisSysbenchTransactions;

                    lastMs = now;
                }
            }

            // shutdown all the writers
            allDone = 1;
        }
    }


    public static void logMe(String format, Object... args) {
        System.out.println(Thread.currentThread() + String.format(format, args));
    }
}
