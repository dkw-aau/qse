package cs.others.extras;

import java.io.IOException;
import java.nio.file.Path;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class EmbeddedNeo4j
{
    private static final Path databaseDirectory = Path.of( "target/neo4j-hello-db" );
    
    public String greeting;
    
    // tag::vars[]
    GraphDatabaseService graphDb;
    Node firstNode;
    Node secondNode;
    Relationship relationship;
    private DatabaseManagementService managementService;
    // end::vars[]
    
    // tag::createReltype[]
    private enum RelTypes implements RelationshipType
    {
        KNOWS
    }
    // end::createReltype[]
    
    public static void main( final String[] args ) throws IOException
    {
        
        EmbeddedNeo4j hello = new EmbeddedNeo4j();
        hello.createDb();
        //hello.removeData();
    
       
  
        //hello.shutDown();
    }
    
    void createDb() throws IOException
    {
        FileUtils.deleteDirectory( databaseDirectory );
        
        // tag::startDb[]
        managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
        registerShutdownHook( managementService );
        // end::startDb[]
        
        // tag::transaction[]
        try ( Transaction tx = graphDb.beginTx() )
        {
            // Database operations go here
            // end::transaction[]
            // tag::addData[]
            firstNode = tx.createNode();
            firstNode.setProperty( "message", "Hello, " );
            secondNode = tx.createNode();
            secondNode.setProperty( "message", "World!" );
            
            relationship = firstNode.createRelationshipTo( secondNode, RelTypes.KNOWS );
            relationship.setProperty( "message", "brave Neo4j " );
            // end::addData[]
            
            // tag::readData[]
            System.out.print( firstNode.getProperty( "message" ) );
            System.out.print( relationship.getProperty( "message" ) );
            System.out.print( secondNode.getProperty( "message" ) );
            // end::readData[]
            
            greeting = ( (String) firstNode.getProperty( "message" ) )
                    + ( (String) relationship.getProperty( "message" ) )
                    + ( (String) secondNode.getProperty( "message" ) );
            
            // tag::transaction[]
            tx.commit();
        }
        // end::transaction[]
    }
    
    void removeData()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            // tag::removingData[]
            // let's remove the data
            firstNode = tx.getNodeById( firstNode.getId() );
            secondNode = tx.getNodeById( secondNode.getId() );
            firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
            firstNode.delete();
            secondNode.delete();
            // end::removingData[]
            
            tx.commit();
        }
    }
    
    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        // tag::shutdownServer[]
        managementService.shutdown();
        // end::shutdownServer[]
    }
    
    // tag::shutdownHook[]
    private static void registerShutdownHook( final DatabaseManagementService managementService )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                managementService.shutdown();
            }
        } );
    }
    // end::shutdownHook[]
}