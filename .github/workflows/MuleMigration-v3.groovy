import groovy.xml.*
import groovy.io.FileType
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

// Load the pom.xml
def pomFile = new File("pom.xml")
def xml = new XmlSlurper( false, false ).parse(pomFile)

def newVersion = "2.0.1"

//xml.appendNode {
   // pluginRepositories {
        //pluginRepository {
           // id 'synergian-repo'
            //url 'https://raw.github.com/synergian/wagon-git/releases'
        //}
    //}
//}

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

// Update dependency version
//xml.dependencies.dependency.each{ dependency ->
   // if(dependency.groupId == "org.mule.connectors" && dependency.artifactId == "mule-http-connector"){
       // dependency.version = newVersion
       // println "Updated my-library version to $newVersion"
    //}
//}

def depData = new JsonSlurper().parseText(new File(".github/workflows/Dependency-Config.json").text)

depData.dependencies.each{ dep ->
    xml.dependencies.dependency.each{ dependency ->
    if(dependency.groupId == dep.groupId && dependency.artifactId == dep.artifactId){
        dependency.version = dep.version
        println "Updated my-library version to $newVersion"
    }
}
    }

// Check if the target node exists
def targetNode1 = xml.dependencies.'*'.find{ it.groupId == 'org.grails.plugins' }

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
// Add new dependency
//xml.dependencies.appendNode {
    //dependency {
        //groupId 'org.grails.plugins'
        //artifactId 'tomcat'
        //version '7.0.42'
        //type 'zip'
        //scope 'provided'
    //}
//}

XmlUtil.serialize(xml, new PrintWriter(new File("pom.xml")))


println "Finished Updating pom.xml"

// Load the artifact.json file

def jsonData = new JsonSlurper().parseText(new File("mule-artifact.json").text)

jsonData.minMuleVersion = "4.9.0"
jsonData.javaSpecificVersions = ["17"]

println "Min Mule Version = ${jsonData.minMuleVersion}"

new File("mule-artifact.json").write(new JsonBuilder(jsonData).toPrettyString())

println "Finished Updating artifact.json"

def folderPath = "src/main/mule" // Replace with the actual path
def searchString = "Hello World" // Replace with the string to search for
def elementToModify = "elementToModify" // Replace with the element to modify
def newValue = "Hi World" // Replace with the new value
//def outputFile = new File(".github/workflows/output_file.txt")
def folder = new File(folderPath)

folder.eachFile(FileType.FILES) { File file ->
    if (file.name.toLowerCase().endsWith(".xml")) {
        //def configXml = new XmlSlurper().parse(file)
	//def xmlFile = new File(file) 
	    def configXml = file.getText()
        // Find the element containing the search string
       // def element = configXml.find { it.text() == searchString }
	    if (configXml.contains(searchString)) {
		    
    println "String '$searchString' found in the file."
def modifiedContent = configXml.replaceAll(searchString, newValue)
 println "$modifiedContent"
  def xmlPrinter = new XmlNodePrinter()
     def xmlString = xmlPrinter.print(modifiedContent)
     //new File("path/to/your/output.xml").write(xmlString)
		    XmlUtil.serialize(xmlString, new PrintWriter(file))
//XmlUtil.serialize(modifiedContent, new PrintWriter(file))
// Write the modified content back to the file
//outputFile.text = modifiedContent
} else {
    println "String '$searchString' not found in the file."
}
	//configXml.replaceAll(searchString,newValue);
      
    }
}

