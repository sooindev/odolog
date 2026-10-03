// 화면 문구 한국어판. 영어판(en.ts)은 이 모양을 그대로 따라야 컴파일됨
// 끼워 넣는 값은 함수 인자. 숫자·금액·단위 표기는 호출하는 쪽이 f 로 만들어 넘김

export const ko = {
  app: {
    name: '오도로그',
    description: '차량 정비 이력을 기록하고 다음 정비 시점을 확인하는 앱',
  },

  common: {
    save: '저장',
    saving: '저장 중…',
    saved: '저장했습니다.',
    cancel: '취소',
    edit: '수정',
    delete: '삭제',
    deleting: '삭제 중…',
    register: '등록',
    registering: '등록 중…',
    loading: '불러오는 중…',
    processing: '처리 중…',
    optional: '선택',
    noChanges: '변경된 내용이 없습니다.',
    previousPage: '이전 페이지',
    nextPage: '다음 페이지',
    showAsTable: '표로 보기',
    showAsValues: '값으로 보기',
    none: '없음',
    email: '이메일',
    password: '비밀번호',
    nickname: '닉네임',
    emailPlaceholder: 'you@example.com',
    /** 숫자 뒤 '건'. 이미 라벨이 기록임을 말하는 자리 */
    count: (n: string) => `${n}건`,
    records: (n: number) => `${n}건`,
  },

  theme: {
    group: '화면 모드',
    light: '라이트 모드',
    system: '시스템 설정 따름',
    dark: '다크 모드',
  },

  header: {
    login: '로그인',
    logout: '로그아웃',
  },

  footer: {
    privacy: '개인정보처리방침',
    terms: '이용약관',
  },

  dateWheel: {
    change: '변경',
    done: '완료',
    year: '년',
    month: '월',
    day: '일',
  },

  errors: {
    network: '서버에 연결하지 못했습니다. 네트워크와 백엔드 실행 상태를 확인해 주세요.',
    http: (status: number) => `요청에 실패했습니다 (HTTP ${status})`,
    /** code 별 문구. field·분이 필요한 것은 함수 */
    codes: {
      VALIDATION_FAILED: (field: string) => `${field} 값을 확인해 주세요.`,
      MALFORMED_BODY: (field: string) => `${field} 값의 형식이 올바르지 않습니다.`,
      INVALID_PARAMETER: (field: string) => `${field} 값이 올바르지 않습니다.`,
      INVALID_SORT: '정렬할 수 없는 항목입니다.',
      FUTURE_DATE: '오늘 이후 날짜는 입력할 수 없습니다.',
      UNSUPPORTED_TIME_ZONE: '지원하지 않는 시간대입니다.',
      UNSUPPORTED_CURRENCY: '지원하지 않는 통화입니다.',
      SAME_PASSWORD: '새 비밀번호가 현재 비밀번호와 같습니다.',
      BAD_REQUEST: '잘못된 요청입니다.',
      LOGIN_REQUIRED: '로그인이 필요합니다.',
      LOGIN_FAILED: '이메일 또는 비밀번호가 올바르지 않습니다.',
      WRONG_PASSWORD: '현재 비밀번호가 올바르지 않습니다.',
      RESET_LINK_INVALID: '링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요.',
      FORBIDDEN: '권한이 없습니다.',
      CSRF_REJECTED: '요청을 확인할 수 없습니다. 새로고침 후 다시 시도해 주세요.',
      VEHICLE_NOT_FOUND: '존재하지 않는 차량입니다.',
      MAINTENANCE_RECORD_NOT_FOUND: '존재하지 않는 정비 이력입니다.',
      FUEL_RECORD_NOT_FOUND: '존재하지 않는 주유 기록입니다.',
      NOT_FOUND: '요청한 주소를 찾을 수 없습니다.',
      METHOD_NOT_ALLOWED: '허용되지 않는 요청 방식입니다.',
      UNSUPPORTED_MEDIA_TYPE: '지원하지 않는 요청 형식입니다.',
      PAYLOAD_TOO_LARGE: '보낸 내용이 너무 큽니다. 파일 크기를 확인해 주세요.',
      EMAIL_DUPLICATE: '이미 가입된 이메일입니다.',
      PLATE_DUPLICATE: '이미 등록하신 차량 번호입니다.',
      ODOMETER_DECREASE: '주행거리는 줄어들 수 없습니다.',
      DUPLICATE_VALUE: '이미 등록된 값입니다. 새로고침 후 다시 시도해 주세요.',
      CONCURRENT_UPDATE: '다른 곳에서 먼저 바뀌었습니다. 새로고침 후 다시 시도해 주세요.',
      TOO_MANY_LOGIN_ATTEMPTS: (minutes: number) =>
        `로그인 시도가 너무 많습니다. ${minutes}분 후 다시 시도해 주세요.`,
      TOO_MANY_SIGNUP_ATTEMPTS: (minutes: number) =>
        `회원가입 시도가 너무 많습니다. ${minutes}분 후 다시 시도해 주세요.`,
      TOO_MANY_RESET_REQUESTS: (minutes: number) =>
        `비밀번호 재설정 요청이 너무 많습니다. ${minutes}분 후 다시 시도해 주세요.`,
      TOO_MANY_PASSWORD_ATTEMPTS: (minutes: number) =>
        `비밀번호를 너무 많이 틀렸습니다. ${minutes}분 후 다시 시도해 주세요.`,
      SAVE_FAILED: '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      SERVER_ERROR: '서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    },
    /** 서버가 알려준 입력 칸 이름 → 화면 이름 */
    fields: {
      email: '이메일',
      password: '비밀번호',
      newPassword: '새 비밀번호',
      currentPassword: '현재 비밀번호',
      nickname: '닉네임',
      plateNumber: '번호판',
      manufacturer: '제조사',
      modelName: '모델명',
      modelYear: '연식',
      odometer: '주행거리',
      serviceOdometer: '주행거리',
      serviceDate: '정비 날짜',
      fueledAt: '주유 날짜',
      liters: '주유량',
      totalCost: '결제 금액',
      cost: '비용',
      memo: '메모',
      description: '메모',
      type: '정비 종류',
      intervalKm: '주행거리 주기',
      intervalMonths: '기간 주기',
      timeZone: '시간대',
      currency: '통화',
      language: '언어',
      unitSystem: '단위',
    } as Record<string, string>,
  },

  password: {
    hint: '8자 이상 · 한글은 24자까지',
    tooLong: (limit: number, bytes: number) =>
      `${limit}바이트를 넘었습니다 (현재 ${bytes}바이트 · 한글은 글자당 3바이트)`,
    mismatch: '새 비밀번호가 서로 다릅니다.',
    newPassword: '새 비밀번호',
    confirm: '새 비밀번호 확인',
    current: '현재 비밀번호',
  },

  login: {
    title: '로그인',
    description: '기록해 둔 차량을 이어서 관리합니다.',
    submit: '로그인',
    submitting: '로그인 중…',
    failed: '로그인에 실패했습니다.',
    noAccount: '계정이 없으신가요?',
    signUp: '회원가입',
    forgot: '비밀번호를 잊으셨나요?',
    notices: {
      signedUp: '가입했습니다. 로그인해 주세요.',
      passwordReset: '비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.',
    },
  },

  signUp: {
    title: '회원가입',
    description: '차량 한 대만 있으면 바로 시작할 수 있습니다.',
    submit: '회원가입',
    submitting: '가입 중…',
    failed: '회원가입에 실패했습니다.',
    haveAccount: '이미 계정이 있으신가요?',
    login: '로그인',
    agreement: (terms: string, privacy: string) => `가입하면 ${terms}과 ${privacy}에 동의하게 됩니다.`,
  },

  forgotPassword: {
    title: '비밀번호 재설정',
    description: '가입할 때 쓴 주소로 재설정 링크를 보냅니다.',
    hint: '가입할 때 쓴 주소로 재설정 링크를 보냅니다.',
    submit: '재설정 링크 받기',
    submitting: '보내는 중…',
    failed: '요청에 실패했습니다.',
    sent: '가입된 주소라면 재설정 링크를 보냈습니다. 메일함을 확인해 주세요.',
    sentDetail:
      '링크는 30분 동안만 쓸 수 있고, 한 번 쓰면 사라집니다. 메일이 오지 않으면 스팸함을 확인하거나 잠시 후 다시 요청해 주세요.',
    backToLogin: '로그인으로 돌아가기',
  },

  resetPassword: {
    title: '새 비밀번호',
    description: '8자 이상으로 정해 주세요. 바꾼 뒤 다시 로그인합니다.',
    noTokenDescription: '메일로 받은 링크에서만 들어올 수 있습니다.',
    invalidLink: '재설정 링크가 올바르지 않습니다.',
    invalidLinkDetail: '메일에 있는 링크를 그대로 눌러 주세요. 링크가 만료됐다면 다시 요청할 수 있습니다.',
    requestAgain: '재설정 링크 다시 받기',
    submit: '비밀번호 변경',
    submitting: '변경 중…',
    failed: '비밀번호 재설정에 실패했습니다.',
  },

  profile: {
    title: '내 정보',
    account: {
      title: '계정',
      description: '닉네임을 바꿀 수 있습니다. 이메일은 변경할 수 없습니다.',
      failed: '저장에 실패했습니다.',
    },
    password: {
      title: '비밀번호',
      description: '바꾸려면 현재 비밀번호를 함께 입력해야 합니다. 변경해도 로그인은 유지됩니다.',
      submit: '비밀번호 변경',
      submitting: '변경 중…',
      changed: '비밀번호를 변경했습니다.',
      failed: '비밀번호 변경에 실패했습니다.',
    },
    region: {
      title: '언어와 단위',
      description:
        '화면 언어, 거리·연료 단위, 통화, 시간대를 정합니다. "오늘" 과 정비 시기는 이 시간대로 판단합니다.',
      language: '언어',
      unitSystem: '단위',
      currency: '통화',
      timeZone: '시간대',
      currencyHint:
        '이미 적은 기록은 원래 통화로 남습니다. 합계에는 지금 통화의 기록만 들어갑니다.',
      timeZoneHint: (browser: string) => `이 기기의 시간대: ${browser}`,
      failed: '설정을 저장하지 못했습니다.',
    },
    appearance: {
      title: '화면',
      description: '라이트·다크 중 하나를 고르거나, 기기 설정을 그대로 따를 수 있습니다.',
      mode: '화면 모드',
      followsSystem: (dark: boolean) => `기기 설정을 따릅니다. 지금은 ${dark ? '다크' : '라이트'}입니다.`,
      fixed: (dark: boolean) => `${dark ? '다크' : '라이트'}로 고정되어 있습니다.`,
    },
    data: {
      title: '내 기록',
      description: '차량·정비 이력·주유 기록을 JSON 파일 하나로 내려받습니다. 비밀번호는 담기지 않습니다.',
      exportNote: '탈퇴하면 기록은 복구되지 않습니다. 지우기 전에 받아 두세요.',
      export: 'JSON 내려받기',
      exporting: '준비 중…',
      exportFailed: '내보내기에 실패했습니다.',
      importNote: '받아 둔 파일을 다시 넣습니다. 같은 기록은 건너뛰므로 두 번 넣어도 늘지 않습니다.',
      import: 'JSON 가져오기',
      importing: '가져오는 중…',
      importFailed: '가져오기에 실패했습니다.',
      notOurFile: '오도로그에서 내려받은 JSON 파일이 맞는지 확인해 주세요.',
      imported: (vehicles: number, records: number) => `차량 ${vehicles}대와 기록 ${records}건을 넣었습니다.`,
      merged: (n: number) => ` 이미 있던 차량 ${n}대에는 기록만 붙였습니다.`,
      intervals: (n: number) => ` 차량별 정비 주기 ${n}개도 되살렸습니다.`,
      skipped: (n: number) => ` 이미 같은 기록이 있어 ${n}건은 건너뛰었습니다.`,
    },
    withdraw: {
      title: '회원 탈퇴',
      description: '계정과 등록한 차량, 정비 이력과 주유 기록이 모두 삭제됩니다. 되돌릴 수 없습니다.',
      note: '탈퇴하면 같은 이메일로 다시 가입할 수 있지만, 기록은 복구되지 않습니다.',
      open: '회원 탈퇴',
      passwordHint: '본인 확인을 위해 현재 비밀번호를 입력하세요.',
      submit: '탈퇴하기',
      submitting: '탈퇴 중…',
      failed: '탈퇴에 실패했습니다.',
    },
  },

  units: {
    KM_PER_L: 'km · L · km/L',
    L_PER_100KM: 'km · L · L/100km',
    MPG_US: '마일 · 미국 갤런 · mpg',
    MPG_UK: '마일 · 리터 · 영국 mpg',
  },

  languages: {
    KO: '한국어',
    EN: 'English',
  },

  vehicles: {
    myVehicles: '내 차량',
    modelYear: (year: number) => `${year}년식`,
    unknownYear: '연식 미상',
    overdue: (n: number) => `정비 ${n}건 지남`,
    dueSoon: (n: number) => `정비 ${n}건 곧`,
    list: {
      title: '내 차량',
      count: (n: number) => `${n}대를 관리 중입니다.`,
      register: '차량 등록',
      loadFailed: '차량 목록을 불러오지 못했습니다.',
      emptyTitle: '아직 등록된 차량이 없습니다',
      emptyBody: '지금 타는 차를 그대로 등록하세요. 현재 주행거리와 기억나는 정비 몇 가지만 적으면 다음 정비 시점부터 바로 보입니다.',
      registerFirst: '첫 차량 등록하기',
    },
    form: {
      sectionTitle: '차량 정보',
      sectionDescription:
        '번호판은 내 차량 안에서만 중복되지 않으면 됩니다. 다른 사람이 같은 번호판을 등록해 두었더라도 상관없습니다.',
      plateNumber: '차량 번호',
      plate: '번호판',
      manufacturer: '제조사',
      modelName: '모델명',
      model: '모델',
      modelYear: '연식',
      platePlaceholder: '12가3456',
      manufacturerPlaceholder: '현대',
      modelPlaceholder: '아반떼',
      title: '차량 등록',
      failed: '차량 등록에 실패했습니다.',
      odometer: (unit: string) => `현재 주행거리 (${unit})`,
      odometerHint: '지금 계기판 숫자 그대로. 타던 차라면 새 차처럼 0 부터 시작하지 않아도 됩니다.',
    },
    gettingStarted: {
      title: '시작하기',
      description: '타던 차도 오늘부터 기록하면 됩니다. 아래 순서대로 하면 다음 정비와 연비가 저절로 채워집니다.',
      progress: (done: number, total: number) => `${total}단계 중 ${done}단계 완료`,
      hide: '안내 닫기',
      done: '완료',
      steps: {
        odometer: {
          title: '지금 주행거리 적기',
          body: '계기판 숫자 그대로. 다음 정비와 연비 판정의 기준이 됩니다.',
          action: '적으러 가기',
        },
        services: {
          title: '기억나는 최근 정비 적기',
          body: '언제 했는지만 기억나도 충분합니다. 비용과 그때 주행거리는 몰라도 됩니다.',
          action: '적기',
        },
        fuel: {
          title: '다음 주유 때 기록하기',
          body: '주유할 때 계기판 숫자와 주유량을 적으세요. 첫 기록은 연비의 기준점이 됩니다.',
          action: '주유 기록으로',
        },
        efficiency: {
          title: '두 번째 주유에서 첫 연비',
          body: '두 번째 주유를 적는 순간 그 사이 구간의 연비가 나옵니다. 그다음부터는 주유할 때마다 쌓입니다.',
        },
      },
    },
    detail: {
      loadFailed: '차량을 불러오지 못했습니다.',
      notFound: '차량을 찾을 수 없습니다.',
      deleteConfirm: '이 차량과 정비 이력, 주유 기록이 모두 삭제됩니다. 계속할까요?',
      deleteNote: '삭제하면 정비 이력과 주유 기록도 함께 사라집니다.',
      delete: '차량 삭제',
      deleteFailed: '삭제에 실패했습니다.',
      odometerAnnounce: (value: string) => `주행거리 ${value}`,
    },
    odometer: {
      title: '주행거리 갱신',
      label: (unit: string) => `현재 주행거리 (${unit})`,
      submit: '갱신',
      failed: '주행거리 갱신에 실패했습니다.',
      bigJump: (from: string, to: string) =>
        `${from} 에서 ${to} 로 크게 뜁니다.\n자리수를 확인해 주세요.\n\n이대로 저장할까요?`,
      decrease: (current: string) =>
        `현재 기록된 ${current} 보다 낮습니다.\n계기판을 교체했거나 잘못 입력한 값을 고치는 경우에만 진행하세요.`,
      conflict: (message: string, current: string) => `${message} (현재 ${current})`,
    },
    info: {
      title: '차량 정보',
      unknown: '미상',
      year: (year: number) => `${year}년`,
      yearHint: '비워 두면 바꾸지 않습니다',
      failed: '차량 정보 수정에 실패했습니다.',
    },
  },

  odometerHints: {
    past: (base: string) => `차량에 기록된 ${base} 보다 작습니다. 과거 기록이면 그대로 두세요.`,
    bigJump: (base: string) => `차량에 기록된 ${base} 에서 크게 뜁니다. 자리수를 확인해 주세요.`,
    bigJumpConfirm: (from: string, to: string) =>
      `주행거리가 ${from} 에서 ${to} 로 크게 뜁니다.\n자리수가 틀리면 차량 주행거리가 그 값에 묶입니다.\n\n이대로 저장할까요?`,
  },

  serviceTypes: {
    ENGINE_OIL: '엔진오일',
    TRANSMISSION_FLUID: '미션오일',
    SPARK_PLUG: '점화 플러그',
    TIMING_BELT: '타이밍 벨트',
    COOLANT: '냉각수',
    BRAKE_PAD: '브레이크 패드',
    BRAKE_FLUID: '브레이크액',
    TIRE: '타이어',
    TIRE_ROTATION: '타이어 위치 교환',
    WHEEL_ALIGNMENT: '휠 얼라인먼트',
    AIR_FILTER: '에어 필터',
    CABIN_FILTER: '에어컨 필터',
    BATTERY: '배터리',
    WIPER: '와이퍼',
    OTHER: '기타',
  },

  serviceTypeGroups: {
    engine: '엔진·구동',
    brakes: '제동',
    tires: '타이어·조향',
    consumables: '소모품',
    other: '그 밖',
  },

  maintenance: {
    title: '정비 이력',
    filterLabel: '정비 종류로 거르기',
    allTypes: '전체 종류',
    add: '이력 추가',
    loadFailed: '정비 이력을 불러오지 못했습니다.',
    empty: '아직 등록된 정비 이력이 없습니다.',
    emptyFiltered: (type: string) => `${type} 이력이 없습니다. 위에서 '전체 종류' 로 바꾸면 전부 보입니다.`,
    deleteConfirm: '이 정비 이력을 삭제할까요?',
    deleteFailed: '삭제에 실패했습니다.',
    form: {
      type: '정비 종류',
      date: '정비 날짜',
      odometer: (unit: string) => `정비 시 주행거리 (${unit})`,
      odometerEmpty: '비우면 다음 정비 시점을 날짜로만 계산합니다.',
      cost: (currency: string) => `비용 (${currency})`,
      costEmpty: '모르면 비워 두세요. 비용 합계에서만 빠집니다.',
      memo: '메모',
      memoPlaceholder: '교체한 부품, 정비소 이름 등',
      failed: '저장에 실패했습니다.',
    },
    quick: {
      intro: '마지막으로 언제 했는지 고르세요. 기억나지 않는 항목은 그대로 두면 됩니다.',
      unknown: '모름',
      monthsAgo: (months: number) => (months === 12 ? '1년 전' : `${months}개월 전`),
      pickDate: '날짜 지정',
      odometer: (unit: string) => `그때 주행거리 (${unit}, 모르면 비움)`,
      submit: '저장',
      nothingPicked: '고른 항목이 없습니다. 기억나는 것 하나만 골라도 됩니다.',
      failed: '저장하지 못했습니다.',
      /** 일부만 저장. 저장된 줄은 이미 '모름' 으로 돌아가 있음 */
      partlySaved: (saved: number, reason: string) =>
        `${saved}건은 저장했고 나머지는 저장하지 못했습니다. ${reason}`,
    },
    next: {
      title: '다음 정비 시점',
      description: '종류별 권장 주기와 마지막 정비 기록으로 계산합니다. 지난 것, 곧(1,000km 또는 한 달 안) 할 것 순으로 위에 옵니다.',
      loadFailed: '다음 정비 시점을 불러오지 못했습니다.',
      empty:
        '아직 계산할 이력이 없습니다. 정비 이력을 등록하면 그 종류의 권장 주기로 다음 시점을 알려 드립니다.',
      overdue: '지남',
      dueSoon: '곧',
      interval: '주기',
      intervalCustomized: '주기 변경됨',
      last: (parts: string) => `마지막 ${parts}`,
      or: ' 또는 ',
      noInterval: '권장 주기 없음',
      intervalDistance: (unit: string) => `주행거리 주기 (${unit})`,
      intervalMonths: '기간 주기 (개월)',
      intervalEmptyHint: '비어 있으면 기본값(회색 숫자)을 씁니다.',
      intervalNote: '이 차량에만 적용됩니다. 둘 다 비우면 기본 권장 주기로 돌아갑니다.',
      intervalFailed: '주기를 저장하지 못했습니다.',
    },
  },

  fuel: {
    title: '주유 기록',
    add: '주유 추가',
    loadFailed: '주유 기록을 불러오지 못했습니다.',
    empty: '아직 주유 기록이 없습니다. 두 번째 기록부터 연비가 계산됩니다.',
    deleteConfirm:
      '이 주유 기록을 삭제할까요?\n\n지운 기록의 주유량이 함께 사라져 다음 기록의 연비가 실제보다 높게 나옵니다.',
    deleteFailed: '삭제에 실패했습니다.',
    noLiters: '주유량 없음',
    noEfficiency: '· 연비 계산 안 됨',
    resetPoint: '연비 기준점',
    baseline: '기준 기록',
    fromNext: '· 다음 주유부터 계산',
    suspicious: '확인 필요',
    missing: '기록 빠짐?',
    form: {
      date: '주유 날짜',
      odometer: (unit: string) => `주행거리 (${unit})`,
      odometerEmpty: '주행거리를 적지 않으면 연비를 계산할 수 없습니다.',
      odometerHint: '계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다.',
      volume: (unit: string) => `주유량 (${unit})`,
      volumeEmpty: '비우면 이번 구간의 연비를 계산할 수 없습니다.',
      cost: (currency: string) => `결제 금액 (${currency})`,
      costEmpty: '비우면 유류비 합계에서 빠집니다.',
      pricePer: (price: string, unit: string) => `${unit}당 약 ${price}`,
      memo: '메모',
      memoPlaceholder: '주유소 이름 등',
      failed: '저장에 실패했습니다.',
      confirmIntro: '이대로 저장하면:',
      confirmOutro: '계속할까요?',
      warnSameOdometer: (current: string) =>
        `· 주행거리가 차량의 현재 값(${current})과 같습니다.\n  이번 구간의 연비가 계산되지 않고, 차량 주행거리도 올라가지 않습니다.`,
      warnNoVolume: '· 주유량이 비어 있어 이번 구간의 연비를 계산할 수 없습니다.',
      warnNoCost: '· 결제 금액이 비어 있어 유류비 합계와 단가에서 빠집니다.',
      warnBigJump: (from: string, to: string) =>
        `· 주행거리가 ${from} 에서 ${to} 로 크게 뜁니다.\n  자리수가 틀리면 차량 주행거리가 그 값에 묶입니다.`,
    },
    summary: {
      title: '연비',
      loadFailed: '주유 요약을 불러오지 못했습니다.',
      reset: '연비 초기화',
      unreset: '초기화 해제',
      resetConfirm: '지금까지의 기록을 연비 계산에서 빼고 다시 셉니다. 계속할까요?',
      resetFailed: '연비 초기화에 실패했습니다.',
      afterReset: '연비를 초기화했습니다. 다음 주유 기록부터 다시 계산합니다.',
      firstRecord: '첫 주유 기록은 기준점이 됩니다. 다음 주유 기록부터 연비를 계산합니다.',
      noSegment:
        '아직 연비를 계산할 수 있는 구간이 없습니다. 주행거리가 늘고 주유량이 적힌 기록이 두 건 이어져야 계산됩니다.',
      sinceReset: '연비 초기화 이후 구간만 계산한 값입니다.',
      excluded: (n: number) =>
        `계산할 수 없는 구간 ${n}곳을 평균에서 뺐습니다. 목록에서 \`확인 필요\` 가 붙은 기록의 주행거리나 주유량을 확인해 주세요.`,
      missing: (n: number) =>
        `주유 기록이 빠진 것으로 보이는 구간 ${n}곳을 평균에서 뺐습니다. 목록에서 \`기록 빠짐?\` 이 붙은 구간의 기록을 채워 넣으면 다시 계산됩니다.`,
      otherCurrency: (n: number, currency: string) =>
        `통화가 ${currency} 가 아닌 기록 ${n}건은 총 유류비에서 뺐습니다.`,
      records: '기록',
      distance: '주행',
      volume: '주유량',
      cost: '총 유류비',
      trend: (n: number) => `최근 ${n}회 구간 연비`,
      average: (value: string) => `평균 ${value}`,
      axisNote: '세로축은 0 부터가 아니라 최솟값 근처에서 시작합니다. 절대량이 아니라 변화를 보는 그림입니다.',
      lowerIsBetter: '이 단위는 숫자가 작을수록 연비가 좋습니다.',
    },
  },

  home: {
    title: (nickname: string) => `${nickname}님의 차고`,
    description: '차량과 정비·주유 기록, 들어간 유지비를 한눈에 봅니다.',
    loadFailed: '차고 정보를 불러오지 못했습니다.',
    goRecord: '기록하러 가기',
    myVehicles: '내 차량',
    tiles: {
      vehicles: '대',
      records: '건',
      breakdown: (maintenance: string, fuel: string) => `정비 ${maintenance} · 주유 ${fuel}`,
    },
    otherCurrency: (n: number, currency: string) =>
      `통화가 ${currency} 가 아닌 기록 ${n}건은 금액 합계와 차트에서 뺐습니다.`,
    vehicles: {
      title: '차량별',
      maintenanceCount: (n: number) => `정비 ${n}건`,
      noService: '정비 이력 없음',
    },
    recent: {
      title: '최근 활동',
      empty: '아직 등록된 기록이 없습니다.',
      fuel: '주유',
    },
    empty: {
      title: '내 차고',
      description: '차량을 등록하면 여기에 통계가 모입니다.',
      heading: '아직 등록된 차량이 없습니다',
      body: '새 차가 아니어도 됩니다. 지금 타는 차와 현재 주행거리만 적으면 시작할 수 있고, 주유·정비를 적을 때마다 이 화면에 쌓입니다.',
      registerFirst: '첫 차량 등록하기',
    },
    monthly: {
      title: '지난 12개월 유지비',
      description: '달마다 들어간 정비비와 유류비의 합입니다.',
      axis: '월',
      month: '월',
      count: '건수',
      maintenance: '정비',
      fuel: '주유',
      total: '합계',
      tooltipCount: (month: string, n: number) => `${month} · ${n}건`,
      breakdown: (maintenance: string, fuel: string) => `정비 ${maintenance} · 주유 ${fuel}`,
    },
    byType: {
      title: '정비 종류별 비용',
      description: '전체 기간 합계입니다. 유류비는 포함하지 않습니다.',
      empty: '아직 등록된 정비 이력이 없습니다.',
    },
  },

  landing: {
    headline1: '마지막 정비가 언제였는지,',
    headline2: '다음은 언제인지.',
    lede: '정비 이력과 주유 기록을 남기면, 다음 정비 시점과 연비를 대신 계산합니다.',
    start: '시작하기',
    login: '로그인',
    whatItDoes: '오도로그가 하는 일',
    highlights: {
      maintenance: {
        title: '정비 이력',
        body: '엔진오일 · 미션오일 · 브레이크 · 타이어 등 15가지 종류. 비용과 메모까지 함께 남깁니다.',
      },
      next: {
        title: '다음 정비 시점',
        body: '종류별 권장 주기와 마지막 기록으로 계산합니다. 주행거리와 날짜, 두 기준을 모두 보여줍니다.',
      },
      fuel: {
        title: '주유와 연비',
        body: '주유할 때마다 주행거리와 넣은 양을 적으면 연비가 나옵니다. 유류비도 함께 쌓입니다.',
      },
      odometer: {
        title: '주행거리',
        body: '차량마다 따로 기록합니다. 이전보다 작은 값은 확인을 거쳐야만 저장됩니다.',
      },
    },
    previewTitle: '차 한 대의 기록이 한 화면에',
    previewBody: '주행거리와 연비, 다음 정비 시점, 지난 이력을 따로 찾아다닐 필요가 없습니다.',
    previewPlate: '12가 3456',
    previewModel: '현대 아반떼',
    previewAverage: '평균 연비',
    closing: '차 한 대만 있으면 시작할 수 있습니다.',
    haveAccount: '이미 계정이 있으신가요?',
  },

  legal: {
    eyebrow: 'Legal',
    updated: (date: string) => `최종 수정 ${date}`,
    draftNotice:
      '이 문서는 서비스 공개 전 초안입니다. 운영자 정보와 연락처를 채우고 법률 검토를 거친 뒤 게시해야 합니다.',
    privacy: {
      title: '개인정보처리방침',
      sections: [
        {
          heading: '수집하는 정보',
          body: [
            '계정: 이메일, 닉네임, 비밀번호(복원할 수 없는 해시로만 저장).',
            '설정: 언어, 시간대, 통화, 단위 체계.',
            '기록: 직접 입력한 차량 정보(번호판·제조사·모델·연식·주행거리), 정비 이력, 주유 기록.',
            '전화번호·주소·결제 정보는 받지 않습니다.',
          ],
        },
        {
          heading: '쓰는 목적',
          body: [
            '로그인과 기록 보관, 다음 정비 시점·연비 계산, 비밀번호 재설정 메일 발송에만 씁니다.',
            '광고, 프로필 분석, 제3자 판매에 쓰지 않습니다.',
          ],
        },
        {
          heading: '쿠키',
          body: [
            '로그인 유지용 세션 쿠키와 위조 요청 방지용 쿠키 두 가지만 씁니다. 둘 다 서비스 동작에 꼭 필요한 쿠키이고, 추적·광고 쿠키는 쓰지 않습니다.',
          ],
        },
        {
          heading: '맡기는 곳',
          body: ['비밀번호 재설정 메일은 메일 발송 서비스를 거쳐 나갑니다. 이때 받는 사람의 이메일 주소가 전달됩니다.'],
        },
        {
          heading: '보관 기간과 삭제',
          body: [
            '탈퇴하면 계정과 모든 기록을 즉시 지웁니다. 되돌릴 수 없습니다.',
            '탈퇴 전에 "내 정보 → 내 기록" 에서 전체 기록을 JSON 파일로 내려받을 수 있습니다.',
          ],
        },
        {
          heading: '내 권리',
          body: [
            '언제든 내 정보를 보고, 고치고, 내려받고, 지울 수 있습니다. 대부분은 "내 정보" 화면에서 바로 할 수 있고, 그 밖의 요청은 아래 연락처로 보내 주세요.',
          ],
        },
        {
          heading: '연락처',
          body: ['[운영자 이름] · [연락 이메일]'],
        },
      ],
    },
    terms: {
      title: '이용약관',
      sections: [
        {
          heading: '서비스',
          body: [
            '오도로그는 차량의 정비 이력과 주유 기록을 적어 두고, 다음 정비 시점과 연비를 계산해 보여 주는 서비스입니다.',
          ],
        },
        {
          heading: '계산 결과',
          body: [
            '다음 정비 시점은 일반적인 권장 주기로 계산한 참고값입니다. 실제 정비는 차량 제조사의 설명서와 정비사의 판단을 따르세요.',
            '연비와 합계는 입력한 값으로만 계산하므로, 입력이 틀리면 결과도 틀립니다.',
          ],
        },
        {
          heading: '계정',
          body: [
            '계정 하나는 한 사람이 씁니다. 비밀번호는 스스로 관리해 주세요.',
            '다른 사람의 계정이나 서비스를 방해하는 방식으로 쓰면 이용을 제한할 수 있습니다.',
          ],
        },
        {
          heading: '내 기록',
          body: ['입력한 기록은 이용자의 것입니다. 언제든 내려받거나 탈퇴로 지울 수 있습니다.'],
        },
        {
          heading: '변경',
          body: ['약관을 바꾸면 이 화면에 먼저 알립니다.'],
        },
      ],
    },
  },
}

export type Messages = typeof ko
