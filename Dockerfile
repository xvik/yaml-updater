FROM frolvlad/alpine-glibc:alpine-3.21_glibc-2.41


COPY --chmod=755 tmp/yaml-updater-linux-amd64 /usr/local/bin/yaml-updater

ENTRYPOINT [ "/usr/local/bin/yaml-updater" ]
CMD ["--help"]
