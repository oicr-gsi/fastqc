package ca.on.oicr.seqware.deciders;

import java.util.*;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import net.sourceforge.seqware.pipeline.deciders.BasicDecider;

/**
 * @author mtaschuk@oicr.on.ca
 *
 */
public class FastqQCDecider extends BasicDecider {

    private Map<String, String> pathToType = new HashMap<String, String>();

    public FastqQCDecider() {
        super();
        parser.acceptsAll(Arrays.asList("ini-file"), "Optional: the location of the INI file.").withRequiredArg();
    }

    @Override
    public ReturnValue init() {
        this.setTemplateIniPath("templateworkflow.ini");
        this.setHeader(Header.IUS_SWA);
        this.setMetaType(Arrays.asList("chemical/seq-na-fastq", "chemical/seq-na-fastq-gzip"));


        ResourceBundle rb = PropertyResourceBundle.getBundle("decider");
        List<String> pas = Arrays.asList(rb.getString("parent-workflow-accessions").split(","));
        List<String> cwa = Arrays.asList(rb.getString("check-wf-accessions").split(","));
        this.setWorkflowAccession(rb.getString("workflow-accession"));
        this.setWorkflowAccessionsToCheck(new TreeSet(cwa));
        this.setParentWorkflowAccessions(new TreeSet(pas));

        //allows anything defined on the command line to override the 'defaults' here.
        ReturnValue val = super.init();
        return val;

    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {
        pathToType.put(fm.getFilePath(), fm.getMetaType());
        return super.checkFileDetails(returnValue, fm);
    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        
        //Load the user-defined ini-file
        Map<String, String> iniFileMap = new TreeMap<String, String>();
        if (options.has("ini-file")) {
            MapTools.ini2Map((String) options.valueOf("ini-file"), iniFileMap, false);
        }

        //parse the given paths and check that there are a maximum of 2
        //specify the run ends from the number of files
        List<String> paths = Arrays.asList(commaSeparatedFilePaths.split(","));
        int runEnds = paths.size();
        if (runEnds > 2) {
            Log.error("There are more than 2 FASTQ files?: " + commaSeparatedFilePaths);
        }
        iniFileMap.put("run_ends", runEnds + "");
        
        //handle the first read
        String path = paths.get(0);
        iniFileMap.put("inputs_read_1", path);
        boolean isGzipped = false;
        if (pathToType.get(path).equals("chemical/seq-na-fastq")) {
            isGzipped = false;
        } else if (pathToType.get(path).equals("chemical/seq-na-fastq-gzip")) {
            isGzipped = true;
        } else {
            Log.error("Unknown file type: " + path + " " + pathToType.get(path));
        }

        //if it exists, handle the second read
        if (runEnds == 2) {
            path = paths.get(1);
            iniFileMap.put("inputs_read_2", path);
            if (pathToType.get(path).equals("chemical/seq-na-fastq-gzip") && isGzipped) {
                iniFileMap.put("cat", "zcat");
            } else if (pathToType.get(path).equals("chemical/seq-na-fastq") && !isGzipped) {
                iniFileMap.put("cat", "cat");
            } else {
                Log.error("The two FASTQ files are of different types: " + commaSeparatedFilePaths);
            }

        }
        return iniFileMap;
    }

    @Override
    protected String handleGroupByAttribute(String attribute) {
        return attribute;
    }
}
