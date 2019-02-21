package toschema;

import PrologDB.Tuple;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class ToOOSchemaTest {
    public static final String TestData = "test/toschema/TestData/";
    public static final String Correct = "test/toschema/Correct/";
    public static final String cv = ".class.violet";
    public static final String vp = ".vpl.pl";
    public static final String op = ".ooschema.pl";
    
    public ToOOSchemaTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    public void doit(String filename) { 
        ToOOSchema.main(TestData+filename+vp);
        RegTest.Utility.validate(filename+op, Correct+filename+op,  false);
    }
    
    @Test
    public void abcd2() {
        doit("abcd2");
    }
    
    @Test
    public void nodeEdge2() {
        doit("nodeEdge2");
    }
    
    @Test
    public void nodeEdge3() {
        doit("nodeEdge3");
    }
    
    @Test
    public void violet1() {
        doit("violet1");
    }
}
