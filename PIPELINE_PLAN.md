Goal: Build a Sales Pipeline for Cold Leads
We need a dedicated dashboard to track potential clients without mixing them into our active client lists. A lead can be either a Company or a Contact. The UI must be in Ukrainian.

Phase 1: Enums & Repository

Update CompanyRole to include: LEAD("Лід") and PROSPECT("Потенційний клієнт").

Update ContactRole to include: LEAD("Лід") and PROSPECT("Потенційний клієнт").

Make sure it won't negatively affect existing db of company and contacts, because they all already has a roles. Also, dont forget to create DB migration schema file for this changes 

Phase 2: Service Layer

Add methods to CompanyService to fetch only pipeline companies (where role IN LEAD, PROSPECT).

Add methods to ContactService to fetch only pipeline contacts (where role IN LEAD, PROSPECT).

Ensure existing methods that fetch active companies/contacts for the main CRM tables exclude LEAD and PROSPECT so the main lists stay clean.

Phase 3: The Pipeline Dashboard (WebPipelineController & Thymeleaf)

Create WebPipelineController with a GET /pipeline endpoint. Fetch both the pipeline companies and pipeline contacts.

Create a new HTML template: templates/pipeline/dashboard.html.

Make the dashboard look modern (similar to our homepage and other pages). Use Bootstrap Tabs or a side-by-side grid to show "Компанії (Ліди)" and "Контакти (Ліди)".

Each row should show the name, phone/email, notes and a colored badge showing if they are a "Лід" (Grey) or "Потенційний клієнт" (Blue). Make it possible to press on contact/company and move to their individual page.

Add a link to /pipeline in the main navigation bar.