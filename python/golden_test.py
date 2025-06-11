import os
import shlex
import subprocess
import tempfile

import pytest


def run_command(command):
    subprocess.call(shlex.split(command), shell=False, cwd="..")


@pytest.mark.golden_test("golden/*.yml")
def test_translator_and_machine(golden):
    with tempfile.TemporaryDirectory() as tmpdirname:
        source = os.path.join(tmpdirname, "source.sasm")
        input_stream = os.path.join(tmpdirname, "input.txt")
        output_stream = os.path.join(tmpdirname, "output.txt")
        log_stream = os.path.join(tmpdirname, "comp_log.txt")
        target = os.path.join(tmpdirname, "target.o")

        with open(source, "w", encoding="utf-8") as file:
            file.write(golden["in_source"])
        with open(input_stream, "w", encoding="utf-8") as file:
            file.write(golden["in_stdin"])

        run_command(f'./gradlew asm:run --args="{source} {target}"')
        run_command(f'./gradlew comp:run --args="-p {target} -i {input_stream} -o {output_stream} -l {log_stream}"')

        with open(output_stream, encoding="utf-8") as file:
            output = file.read()

        with open(log_stream, encoding="utf-8") as file:
            log = file.read()

        assert output == golden.out["out_stdout"]
        assert log == golden.out["out_log"]
