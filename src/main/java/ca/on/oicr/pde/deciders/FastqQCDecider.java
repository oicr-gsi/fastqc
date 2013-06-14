package ca.on.oicr.pde.deciders;

import java.util.*;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import java.io.File;

/**
 * @author zhibin.lu@oicr.on.ca
 *
 */
public class FastqQCDecider extends OicrDecider {

    private Map<String, String> pathToType = new HashMap<String, String>();
    private String folder = "seqware-results";
    private String path = "./";
    private String iniFile=null;
    public FastqQCDecider() {
        super();
        parser.acceptsAll(Arrays.asList("ini-file"), "Optional: the location of the INI file.").withRequiredArg();
        parser.accepts("extract", "whether to extract the final QC zip file");
        parser.accepts("output-folder", "Optional: the name of the folder to put the output into relative to the output-path. "
                + "Corresponds to output-dir in INI file. Default: seqware-results").withRequiredArg();
        parser.accepts("output-path", "Optional: the path where the files should be copied to "
                + "after analysis. Corresponds to output-prefix in INI file. Default: ./").withRequiredArg();

    }

    @Override
    public ReturnValue init() {
        this.setHeader(Header.FILE_SWA);
        this.setMetaType(Arrays.asList("chemical/seq-na-fastq", "chemical/seq-na-fastq-gzip"));
        
        //allows anything defined on the command line to override the 'defaults' here.
        ReturnValue val = super.init();

        if (this.options.has("group-by")) {
            Log.error("I think your workflow run will fail. This fastq-qc workflow handles each fastq file at a time. Please do not use 'group-by' option.");
        }
        if (options.has("ini-file")) {
            File file = new File(options.valueOf("ini-file").toString());
            if (file.exists()) {
                iniFile = file.getAbsolutePath();
                Map<String, String> iniFileMap = MapTools.iniString2Map(iniFile);
                folder = iniFileMap.get("output_dir");
                path = iniFileMap.get("output_path");
            } else {
                Log.error("The given INI file does not exist: " + file.getAbsolutePath());
                System.exit(1);
            }

        }
        if (options.has("output-folder")) {
            folder = options.valueOf("output-folder").toString();
        }
        if (options.has("output-path")) {
            path = options.valueOf("output-path").toString();
            if (!path.endsWith("/")) {
                path += "/";
            }
        }


        return val;

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

        //Load the user-defined ini-file
        Map<String, String> iniFileMap = new TreeMap<String, String>();
        if (options.has("ini-file")) {
            MapTools.ini2Map(iniFile, iniFileMap, false);
        }

        //if the command line has 'extract' option, the final zip file will also be extracted.
        if (options.has("extract")) {
            iniFileMap.put("extract", "yes");
        }
        //FastQC workflow only handles one file at a time, so the commaSeparatedFilePaths should be a single file name. Warning has been given
        //at when the class instance was created. It is users' responsibility not to use 'group-by' option.
        iniFileMap.put("input_file", commaSeparatedFilePaths);
        if (folder!=null) iniFileMap.put("output_dir", folder);
        if (path!=null) iniFileMap.put("output_prefix", path);

        return iniFileMap;
    }

}
