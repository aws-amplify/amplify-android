import org.gradle.api.Plugin
import org.gradle.api.Project

class AmplifyTools implements Plugin<Project> {
    void apply(Project project) {
        def doesNodeExist = true
        def doesGradleConfigExist

        def profile = 'default'
        def accessKeyId = null
        def secretAccessKey = null
        def region = null
        def envName = 'amplify'
        def syncEnabled = 'true'

        project.task('verifyNode') {
            try {
                project.exec {
                    commandLine 'which', 'node'
                    standardOutput = new ByteArrayOutputStream()
                }
            } catch (e) {
                doesNodeExist = false
                println("Node is not installed. Visit https://nodejs.org/en/download/ to install it")
            }
        }

        project.task('createAmplifyApp') {
            doesGradleConfigExist = project.file('amplify-gradle-config.json').isFile()
            if (doesNodeExist && !doesGradleConfigExist) {
                project.exec {
                    commandLine 'npx', 'amplify-app', '--platform', 'android'
                }
            }
        }

        project.task('getConfig') {
            def inputConfigFile = project.file('amplify-gradle-config.json')
            if (inputConfigFile.isFile()) {
                def configText = inputConfigFile.text
                def jsonSlurper = new groovy.json.JsonSlurper()
                def configJson = jsonSlurper.parseText(configText)
                profile = configJson.profile
                accessKeyId = configJson.accessKeyId
                secretAccessKey = configJson.secretAccessKeyId
                region = configJson.region
                envName = configJson.envName
                syncEnabled = configJson.syncEnabled
            }
        }

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
                def jsonSlurper = new groovy.json.JsonSlurper()
                def transformConfJson = jsonSlurper.parseText(tranformConfText)

                def resolverConfigMap = ['ResolverConfig':
                                                 ['project':
                                                          ['ConflictHandler'  : 'AUTOMERGE',
                                                           'ConflictDetection': 'VERSION']
                                                 ]
                ]
                if (!syncEnabled) {
                    transformConfJson.remove('ResolverConfig')
                } else if (!transformConfJson.ResolverConfig) {
                    transformConfJson << resolverConfigMap
                }
                def transformConfJsonStr = groovy.json.JsonOutput.toJson(transformConfJson)
                def transformConfJsonStrPretty = groovy.json.JsonOutput.prettyPrint(transformConfJsonStr)
                transformConfFile.write(transformConfJsonStrPretty)
            }
        }

        project.task('modelgen') {
            doLast {
                project.exec {
                    commandLine 'amplify', 'codegen', 'model'
                }
            }
        }

        project.task('amplifyPush') {
            def AWSCLOUDFORMATIONCONFIG
            if (!accessKeyId || !secretAccessKey || !region) {
                AWSCLOUDFORMATIONCONFIG = """{\
\"configLevel\":\"project\",\
\"useProfile\":true,\
\"profileName\":\"$profile\"\
}"""
            } else {
                AWSCLOUDFORMATIONCONFIG = """{\
\"configLevel\":\"project\",\
\"useProfile\":true,\
\"profileName\":\"$profile\",\
\"accessKeyId\":\"$accessKeyId\",\
\"secretAccessKey\":\"$secretAccessKey\",\
\"region\":\"$region\"\
}"""
            }

            def AMPLIFY
            if (!envName) {
                AMPLIFY = """{\
\"envName\":\"amplify\"\
}"""
            } else {
                AMPLIFY = """{\
\"envName\":\"$envName\"\
}"""
            }

            def PROVIDERS = """{\
\"awscloudformation\":$AWSCLOUDFORMATIONCONFIG\
}"""

            doLast {
                def doesLocalEnvExist = project.file('./amplify/.config/local-env-info.json').exists()
                if (doesLocalEnvExist) {
                    project.exec {
                        commandLine 'amplify', 'push', '--yes'
                    }
                } else {
                    project.exec {
                        commandLine 'amplify', 'init', '--amplify', AMPLIFY, '--providers', PROVIDERS, '--yes'
                    }
                }
            }
        }

        project.task('addModelgenToWorkspace') {
            //Open file
            def xml = new XmlParser().parse('./.idea/workspace.xml')
            def RunManagerNode = xml.component.find {
                it.'@name' == 'RunManager'
            } as Node
            def configModelgenCheck = RunManagerNode.children().find {
                it.'@name' == 'modelgen'
            } as Node

            if (!configModelgenCheck) {
                // Nested nodes for modelgen run configuration
                def configurationNode = new Node(null, 'configuration', [name: "modelgen", type:"GradleRunConfiguration", factoryName:"Gradle", nameIsGenerated:"true"])
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
                def methodNode = new Node(configurationNode, 'method', [v:"2"])

                RunManagerNode.append(configurationNode)

                //Save File
                def writer = new FileWriter('./.idea/workspace.xml')

                //Pretty print XML
                groovy.xml.XmlUtil.serialize(xml, writer)
            }
        }

        project.task('addAmplifyPushToWorkspace') {
            //Open file
            def xml = new XmlParser().parse('./.idea/workspace.xml')
            def RunManagerNode = xml.component.find {
                it.'@name' == 'RunManager'
            } as Node
            def configAmplifyPushCheck = RunManagerNode.children().find {
                it.'@name' == 'amplifyPush'
            } as Node

            if (!configAmplifyPushCheck) {
                // Nested nodes for amplifyPush run configuration
                def configurationNode = new Node(null, 'configuration', [name: "amplifyPush", type:"GradleRunConfiguration", factoryName:"Gradle", nameIsGenerated:"true"])
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
                def methodNode = new Node(configurationNode, 'method', [v:"2"])

                RunManagerNode.append(configurationNode)

                //Save File
                def writer = new FileWriter('./.idea/workspace.xml')

                //Pretty print XML
                groovy.xml.XmlUtil.serialize(xml, writer)
            }
        }

        project.createAmplifyApp.dependsOn('verifyNode')
        project.getConfig.dependsOn('createAmplifyApp')
        project.datastoreSync.dependsOn('getConfig')
        project.modelgen.dependsOn('datastoreSync')
        project.amplifyPush.dependsOn('datastoreSync')
    }
}