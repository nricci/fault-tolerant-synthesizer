import xml.etree.ElementTree as ET
import subprocess

default_file = 'output/final_tableau.xml'

current_file = 0

# Parsed XML
xml = None

# Tableau
root_node = None
current_node = None
nodes = dict()

# Parsing & Setup

def parse():
	global xml
	file = open(default_file)
	content = file.read().replace('&&','and').replace('<->','equiv').replace('->','implies')
	xml = ET.fromstring(content)

def setup():
	global nodes, root_node, current_node
	root_node = xml[0]
	current_node = xml[0]
	for node in xml:
		nodes[node.attrib['id']] = node		

# Movement

def succesors(n):
	assert n.tag == 'node'
	result = set()
	for succ in n[1]:
		result.add(nodes[succ.attrib['id']])
	return result

def move(n):
	global current_node
	assert type(n) is str
	new_node = nodes[n]
	current_node = new_node
	return new_node

def id(n):
	return n.attrib['id']


def expand():
	global snapshot
	snapshot.add(current_node)
	snapshot |= succesors(current_node)
	dump()

def show(n):
	move("n" + n.__str__())
	expand()



# Tableau Snapshot

snapshot = set()

def snapshot_init():
	global snapshot	
	snapshot.clear()
	snapshot.add(root_node)

def full_snapshot():
	global snapshot
	for k in nodes:
		snapshot.add(nodes[k])

def node_to_dot(n):
	res = id(n) + "["
	res += "shape="
	res += "box" if n.attrib['type'] == 'And' else "circle"
	res += ","
	res += "style=dotted," if n.attrib['faulty'] == 'true' else ""
	res += "label=\"" + n.attrib['id'] + n[0].text.replace('\t','') + "\"];"

	trans = ""
	trans += reduce(
				lambda x,y:x+y, 
				map(
					lambda x: id(n) + " -> " + x + ";\n" ,
					map(
						id,
						filter(lambda x: x in snapshot, succesors(n))
						#succesors(n)
						)
					)
				,"")
	return (res,trans)

def to_dot():
	res = reduce(lambda x,y: (x[0]+"\n"+y[0],x[1]+"\n"+y[1]),map(node_to_dot,snapshot),('',''))	
	return "digraph { \n" + res[0] + res[1] + "}"

def dump(i=current_file):
	global current_file
	current_file = i
	f = open('output/pyviz' + current_file.__str__() + '.dot','w')
	res = to_dot().replace('and','&&').replace('equiv','<->').replace('implies', '->')
	f.write(res)
	f.close()








def load():
	parse()
	setup()
	snapshot_init()
	dump()
	subprocess.call(['open', 'output/pyviz0.dot'])

load()


#print "\n ----- nodes -----"
#for k in nodes:
#	print k + " -> " + nodes[k].__str__()

#print "\ncurrent_node : " + current_node.__str__()
#print "\nroot_node : " + root_node.__str__()
#print ""
#print 'done.'