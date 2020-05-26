#!/usr/bin/env groovy

//
// Deploy job to Nomad cluster with Levant
// https://github.com/jrasell/levant
//

def call(Map m = [:]) {
    def projectRepo = m.get('repo')
    def resourcePath = m.get('resource')
    def subdirectory = m.get('subdirectory', '.')
    def envName = m.get('env')
    def jobName = m.get('job')
    def vars = m.get('vars', [:])

    // When true, will render a Nomad job from a template then pass to "nomad run" via pipe.
    // This is a temp workaround if a job uses new Nomad features like consul connect/host volume.
    // https://github.com/jrasell/levant/issues/323
    // https://github.com/jrasell/levant/issues/324
    // https://github.com/jrasell/levant/pull/327
    def useNomadRun = m.get('useNomadRun', false)
    def useNomadJobVars = m.get('useNomadJobVars', true)

    assert projectRepo
    assert resourcePath
    assert envName
    assert jobName

    def targetEnv = loadEnv(resourcePath, envName)
    def levantVersion = targetEnv['levant.version']
    def nomadVersion = targetEnv['nomad.version']
    def nomadAddr = targetEnv['nomad.addr']
    def nomadToken = targetEnv['nomad.token']
    def nomadVars = targetEnv['nomad.vars']
    def nomadBranch = targetEnv['nomad.branch']
    assert levantVersion
    assert nomadVersion
    assert nomadAddr
    assert nomadToken
    assert nomadVars
    assert nomadBranch

    git url: projectRepo, branch: nomadBranch

    echo "Deploying job <${jobName}> to env <${envName}>"

    def varFlags = ''
    vars.each {
        k, v -> varFlags += "-var '${k}=${v}' "
    }

    def command  = useNomadRun ? 'render' : 'deploy'
    def options  = useNomadRun ? '' : '-ignore-no-changes'
    def nomadRun = useNomadRun ? '| nomad run -' : ''
    def nomadJobVars = useNomadJobVars ? "-var-file=${subdirectory}/vars/${nomadVars}/${jobName}.yml" : ''

    withLevant(levantVersion) {
        withNomad(nomadVersion) {
            withCredentials([string(variable: 'NOMAD_TOKEN', credentialsId: nomadToken)]) {
                withEnv(["NOMAD_ADDR=${nomadAddr}"]) {
                    sh """
                        levant ${command} \
                        ${options} \
                        ${varFlags} \
                        -var-file=${subdirectory}/vars/common.yml \
                        -var-file=${subdirectory}/vars/${nomadVars}/common.yml \
                        ${nomadJobVars} \
                        ${subdirectory}/jobs/${jobName}.nomad ${nomadRun}
                    """
                }
            }
        }
    }
}
