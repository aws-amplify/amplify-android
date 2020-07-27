import org.gradle.api.Plugin
import org.gradle.api.Project
import org.apache.tools.ant.taskdefs.condition.Os
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.StringEscapeUtils

class AmplifyTools implements Plugin<Project> {
    void apply(Project project) {
        // profile name can be changed in amplify-gradle-config
        def profile = 'default'
        def accessKeyId = null
        def secretAccessKey = null
        def region = null
        def envName = 'amplify'
        def syncEnabled = 'true'
        def gradleConfigFileName = 'amplify-gradle-config.json'

        project.task('createAmplifyApp') {
            def npx = 'npx'

            if (project.file(gradleConfigFileName).exists()) {
                return
            }

            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                npx += '.cmd'
            }

            try {
                project.exec {
                    commandLine npx, 'amplify-app', '--platform', 'android'
                }
            } catch (commandLineFailure) {
                throw new Exception('Node.js is not installed. Visit https://nodejs.org/en/download/ to install it.')
            }
        }

        project.task('getConfig') {
            def inputConfigFile = project.file('amplify-gradle-config.json')
            if (inputConfigFile.isFile()) {
                def configText = inputConfigFile.text
                def jsonSlurper = new JsonSlurper()
                def configJson = jsonSlurper.parseText(configText)
                profile = configJson.profile
                accessKeyId = configJson.accessKeyId
                secretAccessKey = configJson.secretAccessKeyId
                region = configJson.region
                envName = configJson.envName
                syncEnabled = configJson.syncEnabled
            }
        }
        project.getConfig.dependsOn('createAmplifyApp')

        project.task('datastoreSync') {
            def transformConfFile = project.file('amplify/backend/api/amplifyDatasource/transform.conf.json')
            if (project.file('amplify/backend/api').exists()) {
                new File('amplify/backend/api').eachFileRecurse(groovy.io.FileType.FILES) {
                    if (it.name.endsWith('transform.conf.json')) {
                        transformConfFile = project.file(it)
                    }
                }
            }
            if (transformConfFile.isFile()) {
                def tranformConfText = transformConfFile.text
                def jsonSlurper = new JsonSlurper()
                def transformConfJson = jsonSlurper.parseText(tranformConfText)

                def resolverConfigMap = [
                        'ResolverConfig': [
                                'project': [
                                        'ConflictHandler'  : 'AUTOMERGE',
                                        'ConflictDetection': 'VERSION'
                                ]
                        ]
                ]
                if (!syncEnabled) {
                    transformConfJson.remove('ResolverConfig')
                } else if (!transformConfJson.ResolverConfig) {
                    transformConfJson << resolverConfigMap
                }
                def transformConfJsonStr = JsonOutput.toJson(transformConfJson)
                def transformConfJsonStrPretty = JsonOutput.prettyPrint(transformConfJsonStr)
                transformConfFile.write(transformConfJsonStrPretty)
            }
        }
        project.datastoreSync.dependsOn('getConfig')

        project.task('modelgen') {
            def amplify = 'amplify'

            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                amplify += '.cmd'
            }

            doLast {
                project.exec {
                    commandLine amplify, 'codegen', 'model'
                }
            }
        }
        project.modelgen.dependsOn('datastoreSync')

        project.task('amplifyPush') {
            def awsCloudFormationConfig
            if (!accessKeyId || !secretAccessKey || !region) {
                awsCloudFormationConfig = [
                        'configLevel': 'project',
                        'useProfile' : true,
                        'profileName': profile,
                ]
            } else {
                awsCloudFormationConfig = [
                        'configLevel'    : 'project',
                        'useProfile'     : true,
                        'profileName'    : profile,
                        'accessKeyId'    : accessKeyId,
                        'secretAccessKey': secretAccessKey,
                        'region'         : region,
                ]
            }

            def amplifyConfig
            if (!envName) {
                amplifyConfig = JsonOutput.toJson([
                        'envName': 'amplify',
                ])
            } else {
                amplifyConfig = JsonOutput.toJson([
                        'envName': envName,
                ])
            }

            def providersConfig = JsonOutput.toJson([
                    'awscloudformation': awsCloudFormationConfig,
            ])

            doLast {
                def amplify = 'amplify'

                if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                    amplify += '.cmd'
                    amplifyConfig = StringEscapeUtils.escapeJavaScript(amplifyConfig)
                    providersConfig = StringEscapeUtils.escapeJavaScript(providersConfig)
                }

                if (project.file('./amplify/.config/local-env-info.json').exists()) {
                    project.exec {
                        commandLine amplify, 'push', '--yes'
                    }
                } else {
                    project.exec {
                        commandLine amplify, 'init',
                                '--amplify', amplifyConfig,
                                '--providers', providersConfig,
                                '--yes'
                    }
                }
            }
        }
        project.amplifyPush.dependsOn('datastoreSync')

        project.task('addModelgenToWorkspace') {
            if (project.file('./.idea/workspace.xml').exists()) {
                //Open XML file
                def xml = new XmlParser().parse('./.idea/workspace.xml')
                def RunManagerNode = xml.component.find { it.'@name' == 'RunManager' } as Node
                def configModelgenCheck = null
                if (RunManagerNode) {
                    configModelgenCheck = RunManagerNode.children().find {
                        it.'@name' == 'modelgen'
                    } as Node
                }

                if (!configModelgenCheck) {
                    // Nested nodes for modelgen run configuration
                    def configurationNode = new Node(null, 'configuration', [name: "modelgen", type: "GradleRunConfiguration", factoryName: "Gradle", nameIsGenerated: "true"])
                    def externalSystemNode = new Node(configurationNode, 'ExternalSystemSettings')
                    def executionOption = new Node(externalSystemNode, 'option', [name: "executionName"])
                    def projectPathOption = new Node(externalSystemNode, 'option', [name: "externalProjectPath", value: "\$PROJECT_DIR\$"])
                    def externalSystemIdOption = new Node(externalSystemNode, 'option', [name: "externalSystemIdString", value: "GRADLE"])
                    def scriptParametersOption = new Node(externalSystemNode, 'option', [name: "scriptParameters", value: ""])
                    def taskDescriptionsOption = new Node(externalSystemNode, 'option', [name: "taskDescriptions"])
                    def descriptionList = new Node(taskDescriptionsOption, 'list')
                    def taskNamesOption = new Node(externalSystemNode, 'option', [name: "taskNames"])
                    def nameList = new Node(taskNamesOption, 'list')
                    def modelgenOption = new Node(nameList, 'option', [value: "modelgen"])
                    def vmOption = new Node(externalSystemNode, 'option', [name: "vmOptions", value: ""])
                    def systemDebugNode = new Node(configurationNode, 'GradleScriptDebugEnabled', null, true)
                    def methodNode = new Node(configurationNode, 'method', [v: "2"])

                    RunManagerNode.append(configurationNode)

                    //Save File
                    def writer = new FileWriter('./.idea/workspace.xml')

                    //Pretty print XML
                    groovy.xml.XmlUtil.serialize(xml, writer)
                }
            }
        }

        project.task('addAmplifyPushToWorkspace') {
            if (project.file('./.idea/workspace.xml').exists()) {
                //Open file
                def xml = new XmlParser().parse('./.idea/workspace.xml')
                def RunManagerNode = xml.component.find { it.'@name' == 'RunManager' } as Node
                def configAmplifyPushCheck = null
                if (RunManagerNode) {
                    configAmplifyPushCheck = RunManagerNode.children().find {
                        it.'@name' == 'amplifyPush'
                    } as Node
                }

                if (!configAmplifyPushCheck) {
                    // Nested nodes for amplifyPush run configuration
                    def configurationNode = new Node(null, 'configuration', [name: "amplifyPush", type: "GradleRunConfiguration", factoryName: "Gradle", nameIsGenerated: "true"])
                    def externalSystemNode = new Node(configurationNode, 'ExternalSystemSettings')
                    def executionOption = new Node(externalSystemNode, 'option', [name: "executionName"])
                    def projectPathOption = new Node(externalSystemNode, 'option', [name: "externalProjectPath", value: "\$PROJECT_DIR\$"])
                    def externalSystemIdOption = new Node(externalSystemNode, 'option', [name: "externalSystemIdString", value: "GRADLE"])
                    def scriptParametersOption = new Node(externalSystemNode, 'option', [name: "scriptParameters", value: ""])
                    def taskDescriptionsOption = new Node(externalSystemNode, 'option', [name: "taskDescriptions"])
                    def descriptionList = new Node(taskDescriptionsOption, 'list')
                    def taskNamesOption = new Node(externalSystemNode, 'option', [name: "taskNames"])
                    def nameList = new Node(taskNamesOption, 'list')
                    def amplifyPushOption = new Node(nameList, 'option', [value: "amplifyPush"])
                    def vmOption = new Node(externalSystemNode, 'option', [name: "vmOptions", value: ""])
                    def systemDebugNode = new Node(configurationNode, 'GradleScriptDebugEnabled', null, true)
                    def methodNode = new Node(configurationNode, 'method', [v: "2"])

                    RunManagerNode.append(configurationNode)

                    //Save File
                    def writer = new FileWriter('./.idea/workspace.xml')

                    //Pretty print XML
                    groovy.xml.XmlUtil.serialize(xml, writer)
                }
            }
        }
    }
}
