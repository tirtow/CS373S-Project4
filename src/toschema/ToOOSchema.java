package toschema;

import MDELite.Marquee1In_1Out;
import Parsing.TBPrims.SaveToken.Option;
import PrologDB.DB;
import PrologDB.OOSchema;
import PrologDB.SubTableSchema;
import PrologDB.Table;
import PrologDB.TableSchema;
import PrologDB.Tuple;
import java.util.function.Predicate;

public class ToOOSchema {

    static DB db;
    static Table vbox, vassoc;
    static OOSchema dbs;

    public static void main(String... args) {
        Marquee1In_1Out mark = new Marquee1In_1Out(ToOOSchema.class, ".vpl.pl", ".ooschema.pl", args);
        String inputFileName = mark.getInputFileName();
        String outputFileName = mark.getOutputFileName();
        String appName = mark.getAppName();

        // Step 1: read in the database and create (empty) OO schema
        db = DB.readDB(inputFileName);
        dbs = new OOSchema(appName);
        vbox = db.getTableEH("vBox");
        vassoc = db.getTableEH("vAssociation");

        // Step 2: create schemas for tables
        for (Tuple b : vbox.tuples()) {
            TableSchema schema = new TableSchema(b.get("name"));
            if (b.getBool("abst")) {
                schema.makeAbstract();
            }
            
            // Add fields as columns for each table
            String[] fields = b.get("fields").split("%");
            for (String f : fields) {
                f = f.trim();
                if (!f.isEmpty()) {
                    schema.addColumn(f);
                }
            }

            // Add table schema to OO schema
            dbs.addTableSchema(schema);
        }

        // Step 3: add fields to table schemas by associations AND
        //         create subtable hierarchies
        for (Tuple b : vbox.tuples()) {
            TableSchema schema = dbs.getTableSchema(b.get("name"));
            
            // Add associations
            ToOOSchema.addAssociation(schema, "cid1", b.get("id"));
            ToOOSchema.addAssociation(schema, "cid2", b.get("id"));
            
            // Add subtable hierarchies
            SubTableSchema sub = new SubTableSchema(schema);
            int count = ToOOSchema.addSubTables(schema, sub, "cid1", b.get("id"));
            count += ToOOSchema.addSubTables(schema, sub, "cid2", b.get("id"));
            if (count > 0) {
                dbs.addSubTableSchema(sub);
            }
        }
        
        
        // Step 4: Finally, add identifiers to all root (non-sub) tables and print
        dbs.addIdentifiersToAllNonSubtables();
        dbs.print(outputFileName);
    }

    // place static helper routines here.
    
    /**
     * Adds associations to the given schema.
     * @param schema        the schema to associate to
     * @param joinColumn    the column to join on. Either "cid1" or "cid2"
     * @param id            the id of the table to filter to
     */
    private static void addAssociation(TableSchema schema, String joinColumn,
            String id) {
        // If joining on "cid1" then use "role1" and "arrow1" columns and filter
        // to only vAssociations that have a "cid2" that matches the given id.
        String roleColumn = "role1";
        String arrowColumn = "arrow1";
        Predicate<Tuple> filterPred = t->t.get("vAssociation.cid2").equals(id);
        if (joinColumn.equals("cid2")) {
            roleColumn = "role2";
            arrowColumn = "arrow2";
            filterPred = t->t.get("vAssociation.cid1").equals(id);
        }
        
        String[] columns = {"cid1", "role1", "arrow1", "cid2", "role2", "arrow2"};
        Table reducedVassoc = vassoc.project(columns);
        columns = new String[]{"id", "name"};
        Table reduceVbox = vbox.project(columns);
        Table join = reducedVassoc.join(joinColumn, reduceVbox, "id");
        join = join.filter(filterPred);

        for (Tuple t : join.tuples()) {
            if (t.get("vAssociation." + arrowColumn).equals("DIAMOND")) {              
                ToOOSchema.addAssociationColumn(schema, t, roleColumn,
                        "option ");
            } else if (t.get("vAssociation." + arrowColumn).equals(
                    "BLACK_DIAMOND")) {
                ToOOSchema.addAssociationColumn(schema, t, roleColumn, "");
            }
        }
    }
    
    private static int addSubTables(TableSchema schema, SubTableSchema sub,
            String joinColumn, String id) {
        int count = 0;
        String arrowColumn = "arrow2";
        Predicate<Tuple> filterPred = t->t.get("vAssociation.cid2").equals(id);
        if (joinColumn.equals("cid2")) {
            arrowColumn = "arrow1";
            filterPred = t->t.get("vAssociation.cid1").equals(id);
        }
        
        String[] columns = {"cid1", "role1", "arrow1", "cid2", "role2", "arrow2"};
        Table reducedVassoc = vassoc.project(columns);
        columns = new String[]{"id", "name"};
        Table reduceVbox = vbox.project(columns);
        Table join = reducedVassoc.join(joinColumn, reduceVbox, "id");
        join = join.filter(filterPred);
        
        for (Tuple t : join.tuples()) {
            if (t.get("vAssociation." + arrowColumn).equals("TRIANGLE")) {
                sub.addSubTableSchema(dbs.getTableSchema(t.get("vBox.name")));
                count++;
            }
        }
        
        return count;
        
    }
     
    private static void addAssociationColumn(TableSchema schema, Tuple t,
            String roleColumn, String flags) {
        String fieldType = t.get("vBox.name");
        String fieldName = fieldType;
        if (!t.get("vAssociation." + roleColumn).equals("")) {
            fieldName = t.get("vAssociation." + roleColumn);
        }

        for (String name : fieldName.split(",")) {
            schema.addColumn(name + ":" + flags + fieldType);
        }
    }

}
