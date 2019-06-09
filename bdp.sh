
# FIXME Need to test this script and possibly change it so it spawns a new terminal when running the client/visualization
# module so as not to block stdin/stdout

function usage {
    echo "Blockchain Demo Platform (BDP) - simple cryptocurrency with visualization"
    echo "usage: bdp [--version] [--help] <command> [<args>]"
    echo "   "
    echo "Available BDP commands:"
    echo "  update          Update to the newest version"
    echo "  init            Initialize the application (downloading dependencies, building etc"
    echo "  run             Run the BDP client"
    echo "  visualize       Run the visualization module"
}

function display_version {
    echo "BDP version 0.1.0"
}

function parse_args {
  # positional args
  args=()

  # named args
  while [ "$1" != "" ]; do
      case "$1" in
          -v | --version )              display_version;         exit;;
          -h | --help )                 usage;                   exit;; # quit and show usage
          * )                           args+=("$1")             # if no match, add it to the positional args
      esac
      shift # move to next kv pair
  done

  # restore positional args
  set -- "${args[@]}"

  if [[ -z "${args[0]}" ]]; then
    echo "Need to specify the command!"
    usage
    exit;
  fi

  # set positionals to vars
  command="${args[0]}"

  case $command in
    run )       run; exit;;
    update )    update; exit;;
    init )      init; exit;;
    visualize ) visualize; exit;;
    * )         echo "Unknown command!"
  esac
}

function update {
    echo "Updating..."
    git pull origin master
    echo "Done."
}

function init {
    echo "Initializing..."
    ./gradlew build
    npm install
    echo "Done."
}

function visualize {
    echo "Starting the visualization module..."
    npm start
}

function run {
    echo "Starting the BDP client..."
    ./gradlew run
}

parse_args "$@"