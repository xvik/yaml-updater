FROM frolvlad/alpine-glibc:alpine-3.20_glibc-2.34


COPY tmp/yaml-updater-linux-amd64 /usr/local/bin/yaml-updater
RUN chmod +x /usr/local/bin/yaml-updater

ENTRYPOINT [ "/usr/local/bin/yaml-updater" ]
CMD ["--help"]
