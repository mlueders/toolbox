import groovy.io.FileType

List contexts = []
new File(".").eachFile (FileType.FILES) { File file ->
	if (file.name.endsWith("Context.xml")) {
		println "Reading beans from context=${file.name}"
		contexts << new SpringContext(file)
	}
}

Map beanToContextMap = [:]
contexts.each { SpringContext context ->
	context.beans.each { SpringBean bean ->
		beanToContextMap[bean.name] = context.name
	}
}

contexts.each { SpringContext context ->
	context.calculateContextDependencies(beanToContextMap)
}

contexts.each { SpringContext context ->
	println "Dependencies for context ${context.name}"
	context.beanDependenciesByContext.each { entry ->
		println "  ${entry.key} => ${entry.value}"
	}
}


class SpringBean {
	String name
	String context
	List dependencies = []
}

class SpringContext {
	File file
	List beans
	Map beanDependenciesByContext = [:]
	
	SpringContext(File file) {
		this.file = file
		readSpringBeansFromFile()
	}
	
	def readSpringBeansFromFile() {
		beans = []
		List beanList = []
		XmlParser parser = new XmlParser()
		Node root = parser.parseText(file.text)
		root.bean.each { Node beanNode ->
			String beanName = beanNode.'@id'
			if (!beanName) {
				beanName = beanNode.'@name'
			}
			
			if (beanName) {
				SpringBean bean = new SpringBean('name': beanName, 'context': name)
				addDependenciesToBean(beanNode, bean)
				beans << bean
			}
		}
	}
	
	private def addDependenciesToBean(Node beanNode, SpringBean bean) {
		beanNode.property.each { Node property ->
			boolean noDependencyDefined = false
			int dependencyCount = bean.dependencies.size()
			
			if (property.'@ref') {
				bean.dependencies << property.'@ref'
			} else if (property.'@value' != null) {
				noDependencyDefined = true
			} else {
				if (property.value) {
					noDependencyDefined = true
				} else if (property.bean) {
					if (property.bean[0].children().size() == 0) {
						noDependencyDefined = true
					} else {
						addDependenciesToBean(property.bean[0], bean)
					}
				} else if (property.ref) {
					bean.dependencies << property.ref.'@bean'
				} else if (property.map) {
					property.map.entry.each { Node entry ->
						if (entry.'@value-ref') {
							bean.dependencies << entry.'@value-ref'
						}
					}
				} else if (property.list) {
					property.list.ref.each { Node ref ->
						bean.dependencies << ref.'@bean'
					}
				}
			}
			
			if (!noDependencyDefined && (dependencyCount == bean.dependencies.size())) {
				println "No dependency added for property ${property.'@name'}"
			}
		}
	}
	
	void calculateContextDependencies(Map beanToContextMap) {
		beans.each { SpringBean bean ->
			bean.dependencies.each { String dependency ->
				String context = beanToContextMap[dependency]
				if (context != name) {
					addContextDependency(dependency, context)
				}
			}
		}
	}
	
	private void addContextDependency(String beanName, String contextName) {
		if (contextName == null) {
			contextName = "unknown context"
		}
		
		List dependencyList = beanDependenciesByContext[contextName]
		if (dependencyList == null) {
			dependencyList = []
			beanDependenciesByContext[contextName] = dependencyList
		}
		if (!dependencyList.contains(beanName)) {
			dependencyList << beanName
		}
	}
	
	String getName() {
		file.name
	}
	
}