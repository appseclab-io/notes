# Hybrid only: it negotiates, or it fails with an alert
openssl s_client -connect www.example.com:443 \
  -servername www.example.com -groups X25519MLKEM768 \
  </dev/null 2>&1 | grep -E 'Negotiated|alert'

# The same on an FTP server, after AUTH TLS
openssl s_client -connect ftp.example.com:21 \
  -starttls ftp -groups X25519MLKEM768 </dev/null
