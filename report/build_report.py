"""Rebuild the submission PDFs from source notes and actual execution screenshots.
Requires Python, reportlab and Pillow; the Java project has no dependencies.
"""
from pathlib import Path
from xml.sax.saxutils import escape
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'submission'
OUT.mkdir(exist_ok=True)
WIDTH, HEIGHT = A4
MARGIN = 46
CONTENT = WIDTH - 2 * MARGIN
INK = colors.HexColor('#182333')
BLUE = colors.HexColor('#244c73')
LIGHT = colors.HexColor('#edf3f8')
BODY = ParagraphStyle('body', fontName='Helvetica', fontSize=10, leading=14, textColor=INK)
SMALL = ParagraphStyle('small', parent=BODY, fontSize=8.5, leading=11.5)


def paragraph(c, text, y, style=BODY, x=MARGIN, width=CONTENT):
    p = Paragraph(text, style)
    _, h = p.wrap(width, HEIGHT)
    p.drawOn(c, x, y - h)
    return y - h - 9


def heading(c, text, y):
    c.setFillColor(BLUE)
    c.setFont('Helvetica-Bold', 12)
    c.drawString(MARGIN, y - 12, text)
    return y - 28


def start_page(c, number, title, subtitle=''):
    c.setFillColor(BLUE)
    c.rect(0, HEIGHT - 12, WIDTH, 12, fill=1, stroke=0)
    c.setFont('Helvetica-Bold', 22)
    c.setFillColor(INK)
    c.drawString(MARGIN, HEIGHT - 53, title)
    y = HEIGHT - 72
    if subtitle:
        y = paragraph(c, escape(subtitle), y, SMALL)
    c.setStrokeColor(colors.HexColor('#cad5df'))
    c.line(MARGIN, 37, WIDTH - MARGIN, 37)
    c.setFont('Helvetica', 8)
    c.setFillColor(BLUE)
    c.drawString(MARGIN, 24, 'Matteo Giorgetti | CP2 - Interfaces Project | 28 October 2026')
    c.drawRightString(WIDTH - MARGIN, 24, str(number))
    return y - 8


def code(c, lines, y, size=8.6):
    height = 13 * len(lines) + 17
    c.setFillColor(LIGHT)
    c.roundRect(MARGIN, y - height, CONTENT, height, 5, fill=1, stroke=0)
    c.setFillColor(INK)
    c.setFont('Courier', size)
    for i, line in enumerate(lines):
        c.drawString(MARGIN + 10, y - 15 - 13 * i, line)
    return y - height - 11


def node(c, x, y, name, detail, interface=False):
    w, h = 140, 43
    c.setFillColor(LIGHT if interface else colors.white)
    c.setStrokeColor(BLUE)
    c.roundRect(x, y, w, h, 3, fill=1, stroke=1)
    c.setFillColor(INK)
    c.setFont('Helvetica-Bold', 8)
    c.drawCentredString(x + w / 2, y + 27, name)
    c.setFont('Helvetica', 7)
    c.drawCentredString(x + w / 2, y + 12, detail)


def arrow(c, points, dashed=False, hollow=False):
    import math
    c.setStrokeColor(BLUE)
    c.setDash(3, 2) if dashed else c.setDash()
    path = c.beginPath()
    path.moveTo(*points[0])
    for point in points[1:]:
        path.lineTo(*point)
    c.drawPath(path)
    c.setDash()
    x, y = points[-1]
    old_x, old_y = points[-2]
    angle = math.atan2(y - old_y, x - old_x)
    p1 = (x - 7 * math.cos(angle) + 3.5 * math.sin(angle), y - 7 * math.sin(angle) - 3.5 * math.cos(angle))
    p2 = (x - 7 * math.cos(angle) - 3.5 * math.sin(angle), y - 7 * math.sin(angle) + 3.5 * math.cos(angle))
    tip = c.beginPath()
    tip.moveTo(x, y)
    tip.lineTo(*p1)
    tip.lineTo(*p2)
    tip.close()
    c.setFillColor(colors.white if hollow else BLUE)
    c.drawPath(tip, fill=1, stroke=1)


def diagram(c, y):
    c.saveState()
    c.translate(MARGIN, y - 295)
    node(c, 0, 240, 'SecureMessenger', 'send / receive via Cipher')
    node(c, 180, 240, 'Cipher', 'encrypt / decrypt / default check', True)
    node(c, 360, 240, 'KeyGenerator', 'generateKeySegments(seed)', True)
    node(c, 0, 155, 'RoundFunction', 'apply(right32, roundKey48)', True)
    node(c, 180, 155, 'AbstractBlockCipher', 'validate / pad / convert / split')
    node(c, 360, 155, 'Sha256KeyGenerator', 'normalized seed -> eight segments')
    node(c, 0, 70, 'DesRoundFunction', 'E -> XOR -> S-boxes -> P')
    node(c, 180, 70, 'HbeaCipher', 'private final round keys')
    node(c, 360, 70, 'XorCipher', 'private final 64-bit key')
    arrow(c, [(140, 261), (180, 261)])
    arrow(c, [(250, 198), (250, 240)], dashed=True, hollow=True)
    arrow(c, [(430, 198), (430, 240)], dashed=True, hollow=True)
    arrow(c, [(70, 113), (70, 155)], dashed=True, hollow=True)
    arrow(c, [(250, 113), (250, 155)], hollow=True)
    arrow(c, [(430, 113), (320, 172)], hollow=True)
    arrow(c, [(180, 92), (153, 92), (153, 176), (140, 176)], dashed=True)
    arrow(c, [(320, 92), (344, 92), (344, 261), (360, 261)], dashed=True)
    c.setFillColor(INK)
    c.setFont('Helvetica', 7.5)
    c.drawString(0, 43, 'Hollow triangle: inheritance or implementation. Filled arrow: uses an interface.')
    c.drawString(0, 29, 'Helpers: BitUtils, HbeaTables, InvalidMessageException. Main creates and runs Cipher objects.')
    c.restoreState()
    return y - 298


def screenshot(c, filename, y, caption, bottom=960):
    image = Image.open(ROOT / 'evidence' / filename)
    # Crop only window margins and unused space; all included output is unchanged.
    image = image.crop((112, 77, 1553, bottom))
    path = ROOT / 'report' / (Path(filename).stem + '-crop.png')
    image.save(path)
    h = CONTENT * image.height / image.width
    c.drawImage(str(path), MARGIN, y - h, width=CONTENT, height=h)
    return paragraph(c, escape(caption), y - h - 8, SMALL)


c = canvas.Canvas(str(OUT / 'HBEA_Report.pdf'), pagesize=A4)
c.setTitle('HBEA Interfaces Project - Matteo Giorgetti')
c.setAuthor('Matteo Giorgetti')
y = start_page(c, 1, 'HBEA Interfaces Project', 'Matteo Giorgetti | mgiorgetti.ieu2024@student.ie.edu | Java 21')
y = heading(c, '1. Design and responsibilities', y)
y = paragraph(c, 'The program encrypts one message with two different ciphers. Both follow the same '
              '<b>Cipher</b> interface, so <b>SecureMessenger</b> can send and receive messages without '
              'knowing which algorithm is being used. The diagram shows the main classes and their dependencies.', y)
y = diagram(c, y)
y = paragraph(c, '<b>Shared template.</b> AbstractBlockCipher implements Cipher. It validates plaintext, adds '
              'padding, splits the message into four blocks and converts between bits and hexadecimal. '
              'Its abstract encryptBlock and decryptBlock methods leave the block algorithm to each subclass.', y)
y = paragraph(c, '<b>Interfaces and composition.</b> HbeaCipher receives a KeyGenerator and a RoundFunction. '
              'Sha256KeyGenerator and DesRoundFunction provide the required implementations. XorCipher '
              'inherits the same input/output handling and XORs each block with the first 64 key bits.', y)
y = paragraph(c, '<b>OOP details.</b> Cipher includes the default verifyRoundTrip method. KeyGenerator declares '
              'SEGMENTS and SEGMENT_BITS. Overloading is shown by encrypt(String) and encrypt(int[]). '
              'Overriding methods use @Override. Key fields are private final and have no public getters; '
              'DES table arrays are also private. Main stores the ciphers in a Cipher array.', y)
assert y > 50, y
c.showPage()

y = start_page(c, 2, 'Algorithm and reflections')
y = heading(c, '2. Processing the key and message', y)
y = paragraph(c, '<b>Key generation.</b> Trim the email and convert it to lowercase using Locale.ROOT. '
              'Hash its UTF-8 bytes with SHA-256. Divide the resulting 256 bits into eight consecutive '
              '32-bit segments. Apply DES expansion E to each segment to obtain eight 48-bit round keys.', y)
y = paragraph(c, '<b>Message processing.</b> Reject null, empty or overlong input and characters outside '
              'ASCII 32-126 with the checked InvalidMessageException. Pad valid messages with NUL bytes '
              'to exactly 32 bytes. Convert bytes to bits, most significant bit first, and form four 64-bit blocks.', y)
y = heading(c, '3. Encryption and decryption', y)
y = paragraph(c, 'For each block, apply IP and split it into L0 and R0. Perform eight Feistel rounds with '
              'the equations below. The round function expands R to 48 bits, XORs it with the round key, '
              'uses eight S-boxes to return to 32 bits, then applies P. In each S-box, the outer two bits '
              'select the row and the middle four select the column.', y)
y = code(c, ['L(i) = R(i-1)', 'R(i) = L(i-1) XOR F(R(i-1), RK(i))',
             'F(R, RK) = P(S(E(R) XOR RK))'], y)
y = paragraph(c, 'After round 8, join R8 followed by L8 and apply the final permutation. Join the four '
              'cipher blocks as 64 uppercase hex digits. Decryption uses the same block procedure with '
              'round keys in reverse order. Convert the recovered bits to bytes and remove trailing NUL '
              'padding; ordinary trailing spaces are preserved.', y)
y = heading(c, '4. Reflection questions', y)
y = paragraph(c, '<b>1. Why can decryption work if S-boxes cannot be reversed?</b> The Feistel network '
              'does not need the inverse of F. From L(i) and R(i), the old right half is L(i), and the old '
              'left half is R(i) XOR F(L(i), RK(i)). XOR with the same value twice cancels it. Reversing '
              'the key order and keeping the final swap therefore recovers the original block.', y)
y = paragraph(c, '<b>2. Why are the last three blocks for "Hi Anas" identical?</b> The seven-character '
              'message and one padding byte occupy the first block. Each remaining block contains only '
              'zero bytes. Equal blocks encrypted independently with the same key give equal ciphertext '
              'blocks, revealing repetition and patterns in the message.', y)
y = paragraph(c, '<b>3. Why is a key derived only from an email weak?</b> An email is often public or '
              'easy to guess. Anyone who knows it can compute the same SHA-256 key. Hashing a public '
              'value does not make it a secret. HBEA is a learning exercise, not a safe messaging system.', y)
y = paragraph(c, '<b>4. What changes for AES?</b> Add an AesCipher implementing Cipher and use it in '
              'the new expression that selects the cipher. Cipher method signatures and SecureMessenger '
              'can stay unchanged. A real AES-GCM implementation should handle its own block size, nonce '
              'and authentication tag instead of inheriting this fixed 64-bit block template. The format '
              'would need to carry the nonce and tag, so the HBEA-only 64-hex-digit rule would not apply '
              'to that implementation.', y)
assert y > 50, y
c.showPage()

y = start_page(c, 3, 'Execution screenshots', 'Appendix C | Actual macOS Terminal run using the official university email')
y = screenshot(c, 'demo-screenshot.png', y,
               'Figure 1. Email and message input, both ciphers, recovered plaintext, wrong-key output and repeated blocks.', bottom=943)
y = heading(c, '5. Results from the personalized demo', y)
y = paragraph(c, 'Both HBEA and XOR report <b>Round trip: true</b> for "Meet me at the library at 5 PM!!". '
              'The same SecureMessenger class runs both ciphers. The HBEA ciphertext for the official email is:', y)
y = code(c, ['B16AED86DA4D076D891D90216B221D7CD', '3EBC1077D89A5466E0397D60A6CFA9F'], y)
y = paragraph(c, 'Using a different email produces unreadable text and <b>Matches original: false</b>. '
              'Non-printable output is escaped in the console so that it cannot alter the terminal display. '
              'The last three ciphertext blocks for "Hi Anas" are all <b>683D31912C8623EC</b>, which '
              'demonstrates the independent-block weakness discussed in reflection 2.', y)
assert y > 50, y
c.showPage()

y = start_page(c, 4, 'Testing and round trace')
y = paragraph(c, '<b>TDD evidence.</b> Validation, padding and XOR tests were run against unimplemented '
              'methods first: <b>0 passed, 15 failed</b> (evidence/tdd-red.txt). After implementation, '
              'the corresponding tests pass. Additional BDD scenarios and unit tests check all published '
              'vectors, key normalization, wrong keys, polymorphism and byte order.', y)
y = paragraph(c, '<b>Final run: 69 passed, 0 failed.</b> The checks cover validation/XOR (15), key '
              'generation (13), round vectors (12), cipher scenarios (23) and utilities (6). Within the '
              'cipher scenarios, each cipher also completes 100 deterministic varied-message round trips. '
              'Compilation uses javac -Xlint:all -Werror. Full output is in evidence/tests.txt.', y, SMALL)
y = screenshot(c, 'tests-screenshot.png', y,
               'Figure 2. All 15 initial behavior checks pass; selected BDD results and the full-suite total are shown.', bottom=936)
y = screenshot(c, 'rounds-des-screenshot.png', y,
               'Figure 3. First-letter demo: L and R after every round for M, using the official email. The first encrypted block is 7B8CBE689EF229C7.', bottom=563)
y = paragraph(c, '<b>Reproduce:</b> sh scripts/test.sh; java -cp build Main; '
              'java -cp build RoundTrace mgiorgetti.ieu2024@student.ie.edu. '
              'The separate DES worksheet is supplied as DES_Worksheet_Solution.pdf and DesWorksheet.java.', y, SMALL)
assert y > 45, y
c.save()

# A separate deliverable for the one-page simplified DES class activity.
c = canvas.Canvas(str(OUT / 'DES_Worksheet_Solution.pdf'), pagesize=A4)
c.setTitle('DES Class Activity - Worked Solution - Matteo Giorgetti')
c.setAuthor('Matteo Giorgetti')
y = start_page(c, 1, 'DES class activity', 'Matteo Giorgetti | mgiorgetti.ieu2024@student.ie.edu')
y = paragraph(c, 'The given first-round output is <b>00101011</b>. The second-round key is '
              '<b>10111001</b>. These are the simplified DES tables printed in the activity, '
              'which are different from the full DES tables used in HBEA.', y)
y = heading(c, 'Second round, step by step', y)
steps = [
    ('1. Split and swap', 'L1 = 0010 and R1 = 1011. After the middle swap, L = 1011 and R = 0010.'),
    ('2. Expand R', 'Using E/P = [4, 1, 2, 3, 2, 3, 4, 1], E/P(0010) = 00010100.'),
    ('3. XOR with K2', '00010100 XOR 10111001 = 10101101. Split this into 1010 and 1101.'),
    ('4. Apply S0', 'For 1010: outer bits 10 give row 2; inner bits 01 give column 1. S0[2][1] = 01.'),
    ('5. Apply S1', 'For 1101: outer bits 11 give row 3; inner bits 10 give column 2. S1[3][2] = 01.'),
    ('6. Apply P4', 'Join S0 and S1 to get 0101. P4 = [2, 4, 3, 1] gives 1100.'),
    ('7. Update the left half', '1011 XOR 1100 = 0111. Keep R = 0010, giving 01110010 before the final permutation. There is no swap after the last round.'),
    ('8. Apply inverse IP', 'Using [4, 1, 3, 5, 7, 2, 8, 6], the final result is 10101100.'),
]
for title, body in steps:
    y = paragraph(c, '<b>' + title + '.</b> ' + body, y)
y = code(c, ['The ciphered text: 10101100'], y, size=13)
y = paragraph(c, '<b>Worksheet clarification.</b> The activity text and its parameter box both give '
              'K2 = 10111001. The red diagram label has only seven bits (1011100), so the complete '
              'eight-bit value is used here. Rows and columns above are numbered from zero.', y)
y = paragraph(c, '<b>Verification.</b> Run java -cp build DesWorksheet. It prints each intermediate '
              'value and checks the final answer. A separate direct calculation also produced 10101100.', y)
assert y > 50, y
c.save()
print('Created HBEA_Report.pdf (4 pages) and DES_Worksheet_Solution.pdf (1 page).')
