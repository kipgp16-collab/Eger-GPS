#!/usr/bin/env python3
"""Extract historical 2016 Orenburg hunting-ground coordinate markers.

Produces POINTS ONLY. Textual routes following roads, rivers and district
boundaries cannot be inferred from coordinate lists. Never auto-close polygons.
"""
import json, re, sys, urllib.request
from html.parser import HTMLParser

SOURCES = {
    "OOУ": "https://orenb-gov.ru/doc/44820",
    "ЗОУ": "https://orenb-gov.ru/doc/44822",
}
class Text(HTMLParser):
    def __init__(self):
        super().__init__(); self.parts=[]
    def handle_data(self, data): self.parts.append(data)
    def get(self): return " ".join(self.parts)

def decimal(deg, minute):
    return float(deg)+float(minute.replace(",","."))/60

# Source documents sometimes list longitude first; the 2016 documents
# use longitude around 50..62 E and latitude around 50..55 N.
PAIR = re.compile(
    r"\\(\\s*(\\d{2})\\s*(?:град\\.|°)\\s*(\\d{1,2}[.,]\\d+)\\s*(?:мин\\.|['′])?\\s*[;,:]\\s*"
    r"(\\d{2})\\s*(?:град\\.|°)\\s*(\\d{1,2}[.,]\\d+)\\s*(?:мин\\.|['′])?\\s*\\)",
    re.I)
LABEL = re.compile(r"(?:ООУ|ЗОУ|ПОУ)\\s*(?:№|N)?\\s*\\d{1,2}[-–]\\d{1,3}",re.I)
def extract(kind,url):
    req=urllib.request.Request(url,headers={"User-Agent":"EgerGPS-historical-research/1.0"})
    with urllib.request.urlopen(req,timeout=30) as res: html=res.read().decode("utf-8","replace")
    parser=Text();parser.feed(html);txt=parser.get()
    features=[];current=None
    for match in re.finditer(r".{0,110}",txt): # preserve natural text order by scanning labels and coordinates below
        pass
    events=sorted([(m.start(),"label",m) for m in LABEL.finditer(txt)]+[(m.start(),"coord",m) for m in PAIR.finditer(txt)],key=lambda x:x[0])
    for _,typ,m in events:
        if typ=="label": current=re.sub(r"\\s+"," ",m.group(0));continue
        a,b,c,d=m.groups();v1=decimal(a,b);v2=decimal(c,d)
        if 49<=v1<=64 and 49<=v2<=56: lon,lat=v1,v2
        elif 49<=v2<=64 and 49<=v1<=56: lon,lat=v2,v1
        else: continue
        features.append({"type":"Feature","geometry":{"type":"Point","coordinates":[round(lon,8),round(lat,8)]},
          "properties":{"area":current,"source":url,"year":2016,"kind":kind,
          "verification":"unverified point extraction; NOT a polygon boundary"}})
    return features
def main():
    features=[]
    for kind,url in SOURCES.items():
        try:
            new=extract(kind,url);print(kind,len(new),file=sys.stderr);features.extend(new)
        except Exception as e:print("FAILED",url,e,file=sys.stderr)
    out={"type":"FeatureCollection","name":"orenburg-2016-historical-control-points",
         "properties":{"warning":"POINTS ONLY. Not hunting-ground polygons. Historic 2016 scheme repealed in 2026."},
         "features":features}
    with open("geodata/orenburg-2016-control-points.geojson","w",encoding="utf8") as f:
        json.dump(out,f,ensure_ascii=False,indent=2)
    if not features:sys.exit("No coordinates extracted: inspect upstream HTML or network access")
if __name__=="__main__":main()
