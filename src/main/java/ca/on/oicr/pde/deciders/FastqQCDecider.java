package ca.on.oicr.pde.deciders;

import java.util.*;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import java.io.File;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;

/**
 * @author zhibin.lu@oicr.on.ca
 *
 */
public class FastqQCDecider extends OicrDecider {

    private Map<String, String> pathToType = new HashMap<String, String>();
//    private String folder = "seqware-results";
//    private String path = "./";
    private String iniFile = null;
    
    //NOTE: order of headers in list matters (group by first, then second, etc)
    private List<Header> orderedListOfHeadersToGroupBy = Arrays.asList(Header.WORKFLOW_RUN_SWA, Header.IUS_SWA);

    public FastqQCDecider() {
        super();
        parser.acceptsAll(Arrays.asList("ini-file"), "Optional: the location of the INI file.").withRequiredArg();
        //parser.accepts("extract", "whether to extract the final QC zip file");
//        parser.accepts("output-folder", "Optional: the name of the folder to put the output into relative to the output-path. "
//                + "Corresponds to output-dir in INI file. Default: seqware-results").withRequiredArg();
//        parser.accepts("output-path", "Optional: the path where the files should be copied to "
//                + "after analysis. Corresponds to output-prefix in INI file. Default: ./").withRequiredArg();

    }

    @Override
    public ReturnValue init() {
        
        this.setMetaType(Arrays.asList("chemical/seq-na-fastq", "chemical/seq-na-fastq-gzip"));

        ret = super.init();

        //allows anything defined on the command line to override the 'defaults' here.

        if (this.options.has("group-by")) {
            Log.error("I think your workflow run will fail. This fastq-qc workflow handles each fastq file at a time. Please do not use 'group-by' option.");
        }

        if (options.has("ini-file")) {
            File file = new File(options.valueOf("ini-file").toString());
            if (file.exists()) {
                iniFile = file.getAbsolutePath();
                Log.stdout("User specified ini file will be used: " + iniFile);
                Map<String, String> iniFileMap = MapTools.iniString2Map(iniFile);
//                folder = iniFileMap.get("output_dir");
//                path = iniFileMap.get("output_path");
            } else {
                Log.error("The given INI file does not exist: " + file.getAbsolutePath());
                ret.setExitStatus(ReturnValue.FILENOTREADABLE);
            }

        }
//        if (options.has("output-folder")) {
//            folder = options.valueOf("output-folder").toString();
//        }
//        if (options.has("output-path")) {
//            path = options.valueOf("output-path").toString();
//            if (!path.endsWith("/")) {
//                path += "/";
//            }
//        }

        return ret;

    }

    @Override
    public Map<String, List<ReturnValue>> separateFiles(List<ReturnValue> vals, String groupBy) {

        Map<String, List<ReturnValue>> map = new HashMap<String, List<ReturnValue>>();

        //group files according to the designated header (e.g. sample SWID)
        for (ReturnValue r : vals) {

            StringBuilder keyBuilder = new StringBuilder();

            //iterate through ordered list of headers to group by
            for (Header h : orderedListOfHeadersToGroupBy) {

                String subKey = r.getAttributes().get(h.getTitle());

                if (subKey != null) {
                    subKey = handleGroupByAttribute(subKey);
                }

                keyBuilder.append(String.format("[%s=%s] ", h.getTitle(), subKey));
            }

            String key = keyBuilder.toString();

            List<ReturnValue> vs = map.get(key);
            if (vs == null) {
                vs = new ArrayList<ReturnValue>();
            }
            vs.add(r);
            map.put(key, vs);
        }

        return map;
    }

    @Override
    protected String handleGroupByAttribute(String attribute) {

        return super.handleGroupByAttribute(attribute);
    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {

        pathToType.put(fm.getFilePath(), fm.getMetaType());
        //FastQC workflow only handles gzipped/unzipped FASTQ format.
        if (!fm.getMetaType().equals("chemical/seq-na-fastq") && !fm.getMetaType().equals("chemical/seq-na-fastq-gzip")) {
            return false;
        }

        return super.checkFileDetails(returnValue, fm);

    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {

        Map<String, String> iniFileMap = super.modifyIniFile(commaSeparatedFilePaths, commaSeparatedParentAccessions);

        //Load the user-defined ini-file
        if (options.has("ini-file")) {
            MapTools.ini2Map(iniFile, iniFileMap, false);
        }

//        //Remove "input_files" from ini file - FastQC workflow only one input at a time.
//        //"input_files" is added to the iniFileMap by BasicDecider (parent of OicrDecider).
//        iniFileMap.remove("input_files");

        //if the command line has 'extract' option, the final zip file will also be extracted.
//        if (options.has("extract")) {
//            iniFileMap.put("extract", "yes");
//        }
//        //FastQC workflow only handles one file at a time, so the commaSeparatedFilePaths should be a single file name. Warning has been given
//        //at when the class instance was created. It is users' responsibility not to use 'group-by' option.
//        iniFileMap.put("input_file", commaSeparatedFilePaths);

        iniFileMap.put("input_files", commaSeparatedFilePaths);

//        if (folder!=null) iniFileMap.put("output_dir", folder);
//        if (path!=null) iniFileMap.put("output_prefix", path);

        return iniFileMap;
    }

    public static void main(String args[]) {
        List<String> params = new ArrayList<String>();
        params.add("--plugin");
        params.add(FastqQCDecider.class.getCanonicalName());
        params.add("--");
        params.addAll(Arrays.asList(args));
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));

    }


}
