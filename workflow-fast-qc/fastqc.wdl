version 1.0

# ======================================================
# Workflow accepts two fastq files, with R1 and R2 reads
# ======================================================
workflow fastQC {
input {
        String? modules = "perl/5.28 java/8 fastqc/0.11.8"
        Array[File]+ inputFastqs
}

scatter (f in inputFastqs) {
 call runFastQC { input: inputFastq = f, modules = modules }
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
}

command <<<
 set -euo pipefail
 FASTQC=$(which fastqc)
 JAVA=$(which java)
 perl $FASTQC ~{inputFastq} --java=$JAVA --noextract --outdir "."
>>>

runtime {
  memory:  "~{jobMemory} GB"
  modules: "~{modules}"
}

output {
  File html_report_file = "~{basename(inputFastq, '.fastq.gz')}_fastqc.html"
  File zip_bundle_file  = "~{basename(inputFastq, '.fastq.gz')}_fastqc.zip"
}
}


