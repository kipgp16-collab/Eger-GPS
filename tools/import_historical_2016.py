#!/usr/bin/env python3
"""Best-effort historical 2016 Orenburg polygons, not legal boundaries."""
import json,re,urllib.request,html,pathlib
from html.parser import HTMLParser
class T(HTMLParser):
 def __init__(self):super().__init__();self.a=[]
 def handle_data(self,s):self.a.append(s)
 def handle_starttag(self,t,a):
  if t in ('p','br','tr','div','h2','h3'):self.a.append(' ')
def get(url):
 q=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'})
 with urllib.request.urlopen(q,timeout=90) as f:raw=f.read().decode('utf-8','replace')
 p=T();p.feed(raw);return html.unescape(' '.join(p.a))
num=r'(\d{2})\s*(?:град\.?|°)\s*(\d{1,2}(?:[.,]\d+)?)\s*(?:мин\.?|[′\x27])?'
pair=re.compile(r'\(\s*'+num+r'\s*[;,]\s*'+num+r'\s*(?:мин\.?)?\)',re.I)
head=re.compile(r'\b(?:ООУ|ЗОУ|ПОУ)\s*(?:N|№)?\s*\d+[-–]\d+\b',re.I)
urls=['https://orenb-gov.ru/doc/44820','https://orenb-gov.ru/doc/44822','https://base.garant.ru/27544806/172a6d689833ce3e42dc0a8a7b3cddf9/']
def xy(m):
 a,b,c,d=m.groups();x=int(a)+float(b.replace(',','.'))/60;y=int(c)+float(d.replace(',','.'))/60
 if 49<=x<=56 and 50<=y<=63:return [round(y,7),round(x,7)]
 if 50<=x<=63 and 49<=y<=56:return [round(x,7),round(y,7)]
 return None
def main():
 features=[];report=[]
 for url in urls:
  try:s=get(url)
  except Exception as e:report.append({'url':url,'error':str(e)});continue
  hs=list(head.finditer(s));n=0
  for i,h in enumerate(hs):
   part=s[h.end():(hs[i+1].start() if i+1<len(hs) else len(s))]
   pts=[]
   for m in pair.finditer(part):
    p=xy(m)
    if p and p not in pts:pts.append(p)
   if len(pts)<3:continue
   name=h.group().replace('N','№')
   features.append({'type':'Feature','geometry':{'type':'Polygon','coordinates':[pts+[pts[0]]]},'properties':{'name':name,'source':url,'year':2016,'status':'APPROXIMATE HISTORICAL - NOT LEGAL','points':len(pts)}})
   n+=1
  report.append({'url':url,'sections':len(hs),'polygons':n})
 out=pathlib.Path('app/src/main/assets');out.mkdir(parents=True,exist_ok=True)
 (out/'orenburg-2016-approx.geojson').write_text(json.dumps({'type':'FeatureCollection','features':features},ensure_ascii=False),encoding='utf8')
 pathlib.Path('geodata/orenburg-2016-import-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
 print(json.dumps({'polygons':len(features),'report':report},ensure_ascii=False))
 if not features:raise SystemExit('No valid polygon sequences extracted')
if __name__=='__main__':main()
