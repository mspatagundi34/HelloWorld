import groovy.xml.*
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

println "Start Updating pom.xml"

// Load the pom.xml
def pomFile = new File("pom.xml")
def xml = new XmlSlurper( false, false ).parse(pomFile)

//Read config file
def configData = new JsonSlurper().parseText(new File(".github/workflows/Config.json").text)
def newVersion = "2.0.1"

xml.properties['app.runtime'] = "4.9.0"
xml.properties['mule.maven.plugin.version'] = "4.3.0"

// Update plugin version
xml.build.plugins.plugin.each{ plugin ->
    if(plugin.groupId == 'org.apache.maven.plugins' && plugin.artifactId == "maven-clean-plugin"){
        plugin.version = "3.2.0"
	plugin.configuration.source = "1.17"
	plugin.configuration.target = "1.17"
        println "Updated $plugin"
   }
}

//def depData = new JsonSlurper().parseText(new File(".github/workflows/Dependency-Config.json").text)

configData.dependencies.each{ conf ->
    xml.dependencies.dependency.each{ dependency ->
    if(dependency.groupId == conf.groupId && dependency.artifactId == conf.artifactId){
        dependency.version = conf.version
        println "Updated artifact $dependency.artifactId  to $newVersion"
    }
}
    }

// Check if the depdendency node exists
def targetNode1 = xml.dependencies.'*'.find{ it.groupId == 'org.grails.plugins' }

// Check if the plugin node exists
def targetNode2 = xml.pluginRepositories.'*'.find{ it.id == 'synergian-repo' }

// Append the new dependency node only if the target node is not present
if(!targetNode1){
   println "Dependency Node Not present"
 // Add new dependency
  xml.dependencies.appendNode {
    dependency {
        groupId 'org.grails.plugins'
        artifactId 'tomcat'
        version '7.0.42'
        type 'zip'
        scope 'provided'
    }
}
}

// Append the new pluginRepository node only if the target node is not present
if(!targetNode2){
   println "PluginRepository Node Not present"
 // Add new PluginRepository
  xml.pluginRepositories.appendNode {
        pluginRepository {
            id 'synergian-repo'
            url 'https://raw.github.com/synergian/wagon-git/releases'
	   snapshots {
		enabled true
	}
        }
    }
}
	// Remove a specific dependency
	def dependencyToRemove = xml.dependencies.find { dependency ->
	    dependency.groupId == "com.mulesoft.modules" && dependency.artifactId == "mule-latency-connector"
	}
	
	if (dependencyToRemove) {
	    println("Removing mule-latency-connector dependency...")
	    pom.dependencies.remove(dependencyToRemove)
	    println("mule-latency-connector dependency removed.")
	} else {
	    println("mule-latency-connector dependency not found.")
	}

XmlUtil.serialize(xml, new PrintWriter(new File("pom.xml")))

println "Finished Updating pom.xml"
//-----------------------------------------------------------------------------------//

println "Start Updating mule-artifact.json"
// Load the artifact.json file
def jsonData = new JsonSlurper().parseText(new File("mule-artifact.json").text)

jsonData.minMuleVersion = "4.9.0"
jsonData.javaSpecificVersions = ["17"]

println "Min Mule Version = ${jsonData.minMuleVersion}"

new File("mule-artifact.json").write(new JsonBuilder(jsonData).toPrettyString())

println "Finished Updating mule-artifact.json"

//-----------------------------------------------------------------------------------//

println "Start Updating mule config xml files"
def folderPath = "src/main/mule" // Replace with the actual path
def searchString = "Hello World" // Replace with the string to search for
def newValue = "Hi World" // Replace with the new value

def folder = new File(folderPath)

folder.eachFile(FileType.FILES) { File file ->
    if (file.name.toLowerCase().endsWith(".xml")) {
        //def configXml = new XmlSlurper().parse(file)
	    def configXml = file.getText()
        // Find the element containing the search string
	    if (configXml.contains(searchString)) {
		    
    println "String '$searchString' found in the file."
def modifiedContent = configXml.replaceAll(searchString, newValue)
 //println "$modifiedContent"
XmlUtil.serialize(modifiedContent, new PrintWriter(file))
} else {
    println "String '$searchString' not found in the file."
}      
    }
}
println "Finished Updating mule config xml files"
