version 1.0

# ======================================================
# Workflow accepts two fastq files, with R1 and R2 reads
# ======================================================
workflow fastQC {
input {
        File fastqR1 
        File? fastqR2
        String? outputFileNamePrefix = ""
        String? r1Suffix = "_R1"
        String? r2Suffix = "_R2"
}
Array[File] inputFastqs = select_all([fastqR1,fastqR2])
String outputPrefixOne = if outputFileNamePrefix == "" then basename(inputFastqs[0], '.fastq.gz') + "_fastqc"
                                                       else outputFileNamePrefix + r1Suffix

call runFastQC as firstMateFastQC { input: inputFastq = inputFastqs[0], outputPrefix = outputPrefixOne }
call renameOutput as firstMateHtml { input: inputFile = firstMateFastQC.html_report_file, extension = "html", customPrefix = outputPrefixOne }
call renameOutput as firstMateZip { input: inputFile = firstMateFastQC.zip_bundle_file, extension = "zip", customPrefix = outputPrefixOne }

if (length(inputFastqs) > 1) {
 String outputPrefixTwo = if outputFileNamePrefix=="" then basename(inputFastqs[1], '.fastq.gz') + "_fastqc"
                                                      else outputFileNamePrefix + r2Suffix
 call runFastQC as secondMateFastQC { input: inputFastq = inputFastqs[1],  outputPrefix = outputPrefixTwo }
 call renameOutput as secondMateHtml { input: inputFile = secondMateFastQC.html_report_file, extension = "html", customPrefix = outputPrefixTwo }
 call renameOutput as secondMateZip { input: inputFile = secondMateFastQC.zip_bundle_file, extension = "zip", customPrefix = outputPrefixTwo }
}



meta {
    author: "Peter Ruzanov"
    email: "peter.ruzanov@oicr.on.ca"
    description: "FastQC 3.0"
}

output {
 File? html_report_R1  = firstMateHtml.renamedOutput
 File? zip_bundle_R1   = firstMateZip.renamedOutput
 File? html_report_R2 = secondMateHtml.renamedOutput
 File? zip_bundle_R2  = secondMateZip.renamedOutput
}

}

# ===================================
#            MAIN STEP
# ===================================
task runFastQC {
input {
        Int?   jobMemory = 6
        File   inputFastq
        String? modules = "perl/5.28 java/8 fastqc/0.11.8"
        String? outputPrefix
}

command <<<
 set -euo pipefail
 FASTQC=$(which fastqc)
 JAVA=$(which java)
 perl $FASTQC ~{inputFastq} --java=$JAVA --noextract --outdir "."
>>>

parameter_meta {
 jobMemory: "Memory allocated to fastqc"
 inputFastq: "Input fastq file, gzipped"
 modules: "Names and versions of required modules"
}

runtime {
  memory:  "~{jobMemory} GB"
  modules: "~{modules}"
}

output {
  File html_report_file = "~{basename(inputFastq, '.fastq.gz')}_fastqc.html"
  File zip_bundle_file  = "~{basename(inputFastq, '.fastq.gz')}_fastqc.zip"
}
}

# =================================================
#      RENAMING STEP - IF WE HAVE CUSTOM PREFIX
# =================================================
task renameOutput {
input {
  File inputFile
  String extension
  String customPrefix
}

parameter_meta {
 inputFile: "Input file, html or zip"
 extension: "Extension for a file (without leading dot)"
 customPrefix: "Prefix for making a file"
}

command <<<
 set -euo pipefail
 if [[ ~{basename(inputFile)} != "~{customPrefix}.~{extension}" ]];then 
   ln -s ~{inputFile} "~{customPrefix}.~{extension}"
 else
   ln -s ~{inputFile}
 fi
>>>

output {
  File? renamedOutput = "~{customPrefix}.~{extension}"
}
}
