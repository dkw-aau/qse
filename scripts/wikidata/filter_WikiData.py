STRING_PATTERN = '"@'
ENGLISH_PATTERN = '"@en '
OTHER_ENGLISH_PATTERN = '"@en-'

big_properties = set()
big_properties.add('<http://www.w3.org/2000/01/rdf-schema#label>')
big_properties.add('<http://www.w3.org/2004/02/skos/core#altLabel>')
big_properties.add('<http://www.w3.org/2004/02/skos/core#prefLabel>')
big_properties.add('<http://schema.org/description>')

SOURCE_FILE = 'latest-truthy.nt'
TARGET_FILE = 'wikidata-prefiltered.nt'

f1 = open(SOURCE_FILE, 'r', encoding='utf-8')
f2 = open(TARGET_FILE, 'w', encoding='utf-8')

while True:
    line = f1.readline()
    if not(line):
        break
    else:
        if line.split()[1] in big_properties: # filter big properties
            continue
        elif STRING_PATTERN not in line:      # not big property and not a string: we write it
            f2.write(line)
        elif ENGLISH_PATTERN in line and OTHER_ENGLISH_PATTERN not in line:   # it is a string so we only write if it's in english
            f2.write(line)

f1.close()
f2.close()