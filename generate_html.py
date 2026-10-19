import markdown
import sys
import codecs

def generate():
    md_file = 'FitPilot_Beta_Project_Status_Report.md'
    html_file = 'FitPilot_Beta_Project_Status_Report.html'
    
    with codecs.open(md_file, 'r', encoding='utf-8') as f:
        md_text = f.read()

    html_content = markdown.markdown(md_text, extensions=['tables'])

    full_html = f"""
    <!DOCTYPE html>
    <html lang="tr">
    <head>
        <meta charset="utf-8">
        <title>FitPilot Beta</title>
        <style>
            @page {{
                size: A4;
                margin: 2cm;
                @bottom-right {{
                    content: counter(page);
                }}
            }}
            body {{
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                color: #333;
                line-height: 1.6;
                margin: 20px;
            }}
            h1, h2, h3 {{
                color: #2c3e50;
            }}
            table {{
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 20px;
            }}
            th, td {{
                border: 1px solid #ddd;
                padding: 8px;
            }}
            th {{
                background-color: #f2f2f2;
                text-align: left;
            }}
            code {{
                background-color: #f8f9fa;
                padding: 2px 4px;
                border-radius: 4px;
                font-family: 'Courier New', Courier, monospace;
            }}
            pre {{
                background-color: #f8f9fa;
                padding: 10px;
                border-radius: 4px;
                overflow-x: auto;
            }}
            hr {{
                border: 0;
                height: 1px;
                background-color: #eee;
                margin: 30px 0;
            }}
        </style>
    </head>
    <body>
        {html_content}
    </body>
    </html>
    """

    with codecs.open(html_file, 'w', encoding='utf-8') as f:
        f.write(full_html)

    print("HTML generated successfully.")

if __name__ == '__main__':
    generate()
