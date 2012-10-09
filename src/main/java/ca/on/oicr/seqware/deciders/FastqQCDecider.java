package ca.on.oicr.seqware.deciders;

import java.util.*;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import net.sourceforge.seqware.pipeline.deciders.BasicDecider;

/**
 * @author zhibin.lu@oicr.on.ca
 *
 */
public class FastqQCDecider extends BasicDecider {

    private Map<String, String> pathToType = new HashMap<String, String>();

    public FastqQCDecider() {
        super();
        parser.acceptsAll(Arrays.asList("ini-file"), "Optional: the location of the INI file.").withRequiredArg();
        parser.accepts("extract"); //whether to extract the final QC zip file

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
        return val;

    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {
        pathToType.put(fm.getFilePath(), fm.getMetaType());
        //FastQC workflow only handles gzipped/unzipped FASTQ format.
        if (!fm.getMetaType().equals("chemical/seq-na-fastq") && !fm.getMetaType().equals("chemical/seq-na-fastq-gzip")) {
            return false;
        } else {
            return super.checkFileDetails(returnValue, fm);
        }

    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {

        //Load the user-defined ini-file
        Map<String, String> iniFileMap = new TreeMap<String, String>();
        if (options.has("ini-file")) {
            MapTools.ini2Map((String) options.valueOf("ini-file"), iniFileMap, false);
        }

        //if the command line has 'extract' option, the final zip file will also be extracted.
        if (options.has("extract")) {
            iniFileMap.put("extract", "yes");
        }
        //FastQC workflow only handles one file at a time, so the commaSeparatedFilePaths should be a single file name. Warning has been given
        //at when the class instance was created. It is users' responsibility not to use 'group-by' option.
        iniFileMap.put("input_file", commaSeparatedFilePaths);

        return iniFileMap;
    }

}