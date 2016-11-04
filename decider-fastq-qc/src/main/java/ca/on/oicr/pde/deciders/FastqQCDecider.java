package ca.on.oicr.pde.deciders;

import java.util.*;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.common.util.maptools.MapTools;
import java.io.File;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;

/**
 * @author zhibin.lu@oicr.on.ca
 *
 */
public class FastqQCDecider extends OicrDecider {

    private String iniFile = null;
    private List<String> skipSequencerRuns = null;

    public FastqQCDecider() {
        super();
        parser.acceptsAll(Arrays.asList("ini-file"), "Optional: the location of the INI file.").withRequiredArg();
        parser.accepts("skip-sequencer-runs", "Optional: comma-separated list of sequencer run names to ignore in this decider run.").withRequiredArg();
    }

    @Override
    public ReturnValue init() {

        this.setMetaType(Arrays.asList("chemical/seq-na-fastq", "chemical/seq-na-fastq-gzip"));
        //this.setHeadersToGroupBy(Arrays.asList(Header.WORKFLOW_RUN_SWA, Header.SEQUENCER_RUN_NAME, Header.LANE_NUM, Header.SAMPLE_NAME, GROUP_ID...));
        this.setHeadersToGroupBy(Arrays.asList(Header.IUS_SWA));

        ReturnValue ret = super.init();

        if (this.options.has("group-by")) {
            Log.error("I think your workflow run will fail. This fastq-qc workflow handles each fastq file at a time. Please do not use 'group-by' option.");
        }

        if (options.has("ini-file")) {
            File file = new File(options.valueOf("ini-file").toString());
            if (file.exists()) {
                iniFile = file.getAbsolutePath();
                Log.stdout("User specified ini file will be used: " + iniFile);
            } else {
                Log.error("The given INI file does not exist: " + file.getAbsolutePath());
                ret.setExitStatus(ReturnValue.FILENOTREADABLE);
            }
        }

        if (options.has("skip-sequencer-runs")) {
            skipSequencerRuns = Arrays.asList(options.valueOf("skip-sequencer-runs").toString().split(","));
        }

        return ret;
    }

    @Override
    protected String handleGroupByAttribute(String attribute) {
        return super.handleGroupByAttribute(attribute);
    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {
        //FastQC workflow only handles gzipped/unzipped FASTQ format.
        if (!fm.getMetaType().equals("chemical/seq-na-fastq") && !fm.getMetaType().equals("chemical/seq-na-fastq-gzip")) {
            return false;
        }
        // SEQWARE-1809, PDE-474 ensure that deciders only use input from completed workflow runs
        String status = returnValue.getAttribute(FindAllTheFiles.WORKFLOW_RUN_STATUS);
        if (status == null || !status.equals("completed")) {
            return false;
        }
        if (skipSequencerRuns != null) {
            String srn = returnValue.getAttribute(Header.SEQUENCER_RUN_NAME.getTitle());
            if (skipSequencerRuns.contains(srn.trim())) {
                return false;
            }
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

        iniFileMap.put("input_files", commaSeparatedFilePaths);

        return iniFileMap;
    }

    public static void main(String args[]) {
        List<String> params = new ArrayList<>();
        params.add("--plugin");
        params.add(FastqQCDecider.class.getCanonicalName());
        params.add("--");
        params.addAll(Arrays.asList(args));
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));
    }

}
