package toschema;

import MDELite.Marquee1In_1Out;
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
                // Class is abstract
                schema.makeAbstract();
            }
            
            // Add fields as columns for each table ignoring empty fields
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
            int count = ToOOSchema.addSubTables(sub, "cid1", b.get("id"));
            count += ToOOSchema.addSubTables(sub, "cid2", b.get("id"));
            
            // Only add subtable schema if actually added subtables
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
            // Joining on "cid2", flip the columns and filter being used.
            roleColumn = "role2";
            arrowColumn = "arrow2";
            filterPred = t->t.get("vAssociation.cid1").equals(id);
        }
        
        // Join using joinColumn from vAssociation and "id" from vBox
        Table join = vassoc.join(joinColumn, vbox, "id");
        
        // Filter to only rows that match the given id
        join = join.filter(filterPred);

        // Add associations for each tuple
        for (Tuple t : join.tuples()) {
            if (t.get("vAssociation." + arrowColumn).equals("DIAMOND")) {   
                // Arrow is an open diamond so association is optional
                ToOOSchema.addAssociationColumn(schema, t, roleColumn,
                        "option ");
            } else if (t.get("vAssociation." + arrowColumn).equals(
                    "BLACK_DIAMOND")) {
                // Arrow is a closed diamond so association is not optional
                ToOOSchema.addAssociationColumn(schema, t, roleColumn, "");
            }
        }
    }
     
    /**
     * Adds a column to the schema
     * @param schema        the schema to add to
     * @param t             the tuple with the column to add
     * @param roleColumn    the column with the role to use as the name
     * @param flags         extra text to add in the column name
     */
    private static void addAssociationColumn(TableSchema schema, Tuple t,
            String roleColumn, String flags) {
        String fieldType = t.get("vBox.name");
        
        // Default the field name to the fieldType unless it is given in
        // roleColumn
        String fieldName = fieldType;
        if (!t.get("vAssociation." + roleColumn).equals("")) {
            fieldName = t.get("vAssociation." + roleColumn);
        }

        // Can have multiple columns to add separated by ",". Add them all
        for (String name : fieldName.split(",")) {
            schema.addColumn(name + ":" + flags + fieldType);
        }
    }
        
    /**
     * Adds the subtables for the given subtable schema.
     * @param sub           the subtable schema to add to
     * @param joinColumn    the column to join on. Either "cid1" or "cid2"
     * @param id            the id of the table to filter to
     * @return the number of subtables added
     */
    private static int addSubTables(SubTableSchema sub, String joinColumn,
            String id) {
        int count = 0;
        
        // If joining on "cid1" then use "arrow2" and "cid2" columns
        String arrowColumn = "arrow2";
        Predicate<Tuple> filterPred = t->t.get("vAssociation.cid2").equals(id);
        if (joinColumn.equals("cid2")) {
            // Joining on "cid2", use "arrow1" and "cid1" columns
            arrowColumn = "arrow1";
            filterPred = t->t.get("vAssociation.cid1").equals(id);
        }
        
        // Join using joinColumn from vAssociation and "id" from vBox
        Table join = vassoc.join(joinColumn, vbox, "id");
        
        // Filter to the given id
        join = join.filter(filterPred);
        
        // Add subtables for each tuple
        for (Tuple t : join.tuples()) {
            if (t.get("vAssociation." + arrowColumn).equals("TRIANGLE")) {
                sub.addSubTableSchema(dbs.getTableSchema(t.get("vBox.name")));
                count++;
            }
        }
        
        return count;
    }
}
