<p style="text-align: center"><img src="./.github/logo.webp" height="180" alt="Interweb Logo"/></p>

# Learnweb - Collaborative learning environment with extended search and sharing functions

<p style="text-align: center">
<a href="https://opensource.org/licenses/MIT" alt="License: MIT">
   <img src="https://img.shields.io/badge/License-MIT-yellow.svg"/></a>
<a href="https://github.com/l3s-learnweb/learnweb/tags" alt="Releases">
   <img src="https://img.shields.io/github/v/tag/l3s-learnweb/learnweb"/></a>
</p>

Learnweb is a collaborative search and sharing system which brings together different online services such as
YouTube, Flickr, Google Search, Vimeo and SlideShare under one umbrella. It also provides advanced features for organizing and
sharing distributed resources with a group of people.

## History

The history of this project starts in 2009 with the work of Dr. Sergej Zerr and his students at the L3S Research Center.
Since that time, the system was continuously developed and improved by many students and researchers, to meet the needs
of all user groups – teachers, learners, researchers.

LearnWeb has been adapted to learning scenarios such as the YELL/TELL community and research projects such as EU-MADE4LL.
YELL/TELL stands for Young English Language Learners/Teen English Language Learners and is a virtual meeting place for
foreign language teachers, language students and university lecturers who can seek, share, evaluate and adapt resources
to professional practice. The EU-MADE4LL project (European Multimodal and Digital Education for Language Learning)
promotes the modernisation of higher education and the employability of graduates.

## Components of Learnweb

To run the project, with all its features, you need the following components:

- A Learnweb web application (this repository)
  - A database: MySQL or MariaDB
  - A servlet container: Apache Tomcat or Jetty
  - Resources index: Apache Solr (used for groups resources navigation and search)  <--- TODO: make it optional
  - File storage: local path to a directory
  - Video thumbnail and conversion tool: FFMPEG (optional, if you want video thumbnails)
  - SMTP account (for sending emails)
  - IMAP account (for bounce handling, optional)
  - Sentry (for error logging, optional)
  - Captcha (Google ReCaptcha or hCaptcha, for registration/login protection, optional)
- [Interweb](https://github.com/l3s-learnweb/interweb) (Search and LLM)
- [ThumbEngine](https://github.com/astappiev/thumbengine) (for generating thumbnails)
- [OnlyOffice](https://github.com/ONLYOFFICE/Docker-DocumentServer) (for document editing)
- Learnweb Tracker (for tracking user activity, optional)
- Archive Save Url (for archiving websites, optional)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
