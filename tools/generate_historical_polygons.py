#!/usr/bin/env python3
"""Generate *rough* historical polygons from downloaded 2016 source texts.
Input: UTF-8 text files in geodata/source-text/*.txt
No polygons are invented for sites lacking three coordinate pairs.
"""
import json,re,pathlib,sys
root=pathlib.Path("geodata")
output=root/"orenburg-2016-approx.geojson"
coord=re.compile(r"([0-9]{2})\s*[°º]\s*([0-9]{1,2}(?:[.,][0-9]+)?)\s*['′]?\s*[,;]\s*([0-9]{2})\s*[°º]\s*([0-9]{1,2}(?:[.,][0-9]+)?)",re.I)
title=re.compile(r"(?:ООУ|ЗОУ|ПОУ)\s*(?:№\s*)?[0-9]+[-–][0-9]+",re.I)
def point(m):
 a,b,c,d=m.groups()
 x=int(a)+float(b.replace(",","."))/60
 y=int(c)+float(d.replace(",","."))/60
 if 49<=x<=56 and 50<=y<=63:return [round(y,7),round(x,7)]
 if 50<=x<=63 and 49<=y<=56:return [round(x,7),round(y,7)]
 return None
def main():
 features=[];summary=[]
 for path in sorted((root/"source-text").glob("*.txt")):
  txt=path.read_text(encoding="utf-8");heads=list(title.finditer(txt));made=0
  for i,h in enumerate(heads):
   chunk=txt[h.end():heads[i+1].start() if i+1<len(heads) else len(txt)]
   vertices=[]
   for m in coord.finditer(chunk):
    p=point(m)
    if p and (not vertices or vertices[-1]!=p):vertices.append(p)
   if len(vertices)>1 and vertices[-1]==vertices[0]:vertices.pop()
   if len(set(map(tuple,vertices)))<3:continue
   features.append({"type":"Feature","geometry":{"type":"Polygon","coordinates":[vertices+[vertices[0]]]},
     "properties":{"name":h.group(0),"source_file":path.name,"year":2016,
     "accuracy":"approximate straight lines; river and road boundaries not reconstructed",
     "legal_status":"repealed historic scheme; not suitable for determining hunting permissions"}})
   made+=1
  summary.append({"file":path.name,"headings":len(heads),"polygons":made})
 output.parent.mkdir(parents=True,exist_ok=True)
 output.write_text(json.dumps({"type":"FeatureCollection","features":features},ensure_ascii=False,indent=2),encoding="utf-8")
 print(json.dumps({"total_polygons":len(features),"sources":summary},ensure_ascii=False))
 if not features:sys.exit("No complete coordinate sequences found. Add historical source text files.")
if __name__=="__main__":main()
